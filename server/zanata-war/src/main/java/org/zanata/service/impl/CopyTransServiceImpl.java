/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.service.impl;

import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;
import org.zanata.common.ContentState;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.TextFlowDAO;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HSimpleComment;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;
import org.zanata.rest.service.TranslationResourcesService;
import org.zanata.service.CopyTransService;
import org.zanata.service.LocaleService;

//TODO unit test suite for this class

@Name("copyTransServiceImpl")
@AutoCreate
@Scope(ScopeType.STATELESS)
public class CopyTransServiceImpl implements CopyTransService
{
   
   private LocaleService localeServiceImpl;
   private TextFlowTargetDAO textFlowTargetDAO;
   private DocumentDAO documentDAO;
   private TextFlowDAO textFlowDAO;
   @Logger
   Log log;

   @In
   public void setLocaleServiceImpl(LocaleService localeService)
   {
      this.localeServiceImpl = localeService;
   }

   @In
   public void setTextFlowTargetDAO(TextFlowTargetDAO tftDAO)
   {
      this.textFlowTargetDAO = tftDAO;
   }

   @In
   public void setDocumentDAO(DocumentDAO documentDAO)
   {
      this.documentDAO = documentDAO;
   }

   @In
   public void setTextFlowDAO(TextFlowDAO textFlowDAO)
   {
      this.textFlowDAO = textFlowDAO;
   }


   @Observer(TranslationResourcesService.EVENT_COPY_TRANS)
   public void execute(Long docId, String project, String iterationSlug)
   {
      HDocument document = documentDAO.findById(docId, true);
      log.info("copyTrans start: document \"{0}\"", document.getDocId());
      String projectName = document.getProjectIteration().getProject().getName();
      List<HLocale> localelist = localeServiceImpl.getSupportedLangugeByProjectIteration(project, iterationSlug);
      log.info("found {0} candidate locales for project \"{1}\"", localelist.size(), projectName);
      // eliminate locales that have no translations for this document
      localelist = textFlowTargetDAO.filterLocalesWithDocumentTranslations(document.getDocId(), localelist);
      log.info("have {0} candidate locales for document \"{1}\" after filtering for available translations", localelist.size(), document.getDocId());
      int copyCount = 0;
      try
      {
         Transaction.instance().begin();
         for (HTextFlow textFlow : document.getTextFlows())
         {
            copyCount += copyTransForTextFlow(textFlow, localelist, document.getDocId(), iterationSlug, projectName);
         }
         textFlowTargetDAO.flush();
         Transaction.instance().commit();

         log.info("copyTrans: {0} translations for document \"{1}{2}\" ", copyCount, document.getPath(), document.getName());
      }
      catch (Exception e)
      {
         log.warn(e, e);
         try
         {
            Transaction.instance().rollback();
         }
         catch (Exception i)
         {
            log.warn(i, i);
         }
      }

      log.info("copyTrans finished: document \"{0}\"", document.getDocId());
   }

   /**
    * Copies translations from matching {@link HTextFlow}s to the given
    * {@link HTextFlow}.
    * 
    * A HTextFlow is considered matching if it is in a document with the same
    * id, has the same resource id, and has the same content.
    * 
    * @param textFlow the text flow for which to copy translations
    * @param localelist locales for which to copy translations
    * @param documentId
    * @param projectIteration
    * @param projectName
    * 
    * @return the number of translations that were copied
    */
   private int copyTransForTextFlow(HTextFlow textFlow, List<HLocale> localelist, String documentId, String projectIteration, String projectName)
   {
      int copyCount = 0;
      List<Long> matchingFlowIds = null;
      for (HLocale locale : localelist)
      {
         HTextFlowTarget hTarget = textFlow.getTargets().get(locale);
         if (hTarget == null || hTarget.getState() != ContentState.Approved)
         {
            // expensive operation deferred until we know it is required
            if (matchingFlowIds == null)
            {
               matchingFlowIds = textFlowDAO.getIdsByMatchingContent(textFlow);
               if (matchingFlowIds.isEmpty())
                  return copyCount; // no equivalent content from which to
                                    // retrieve translations
            }
            HTextFlowTarget oldTFT = textFlowTargetDAO.findLatestApprovedTranslationInCandidateFlows(matchingFlowIds, locale);
            if (oldTFT != null)
            {
               if (hTarget == null)
               {
                  hTarget = new HTextFlowTarget(textFlow, locale);
                  hTarget.setVersionNum(1);
                  textFlow.getTargets().put(locale, hTarget);
               }
               else
               {
                  // DB trigger will copy old value to history table, if
                  // we change the versionNum
                  hTarget.setVersionNum(hTarget.getVersionNum() + 1);
               }
               // NB we don't touch creationDate
               hTarget.setTextFlowRevision(textFlow.getRevision());
               hTarget.setLastChanged(oldTFT.getLastChanged());
               hTarget.setLastModifiedBy(oldTFT.getLastModifiedBy());
               hTarget.setContent(oldTFT.getContent());
               hTarget.setState(oldTFT.getState());
               HSimpleComment hcomment = hTarget.getComment();
               if (hcomment == null)
               {
                  hcomment = new HSimpleComment();
                  hTarget.setComment(hcomment);
               }
               hcomment.setComment(createComment(oldTFT, projectName, projectIteration, documentId));
               textFlowTargetDAO.makePersistent(hTarget);
               ++copyCount;
            }
         }
      }
      return copyCount;
   }

   private String createComment(HTextFlowTarget target, String projectname, String version, String documentid)
   {
      String authorname;
      if (target.getLastModifiedBy() != null)
      {
         authorname = target.getLastModifiedBy().getName();
      }
      else
      {
         authorname = "";
      }
      return String.format("translation auto-copied from project {0}, version {1}, document {2}, author {3}", projectname, version, documentid, authorname);
   }

   // TODO unit testing for this method
   @Override
   public void copyTransForLocale(HDocument document, HLocale locale, String projectname, String version)
   {
      try
      {
         Transaction.instance().begin();
         int copyCount = 0;
         for (HTextFlow textFlow : document.getTextFlows())
         {
            HTextFlowTarget hTarget = textFlow.getTargets().get(locale);
            if (hTarget != null && hTarget.getState() == ContentState.Approved)
               continue;
            HTextFlowTarget oldTFT = textFlowTargetDAO.findLatestEquivalentTranslation(textFlow, locale);
            if (oldTFT != null)
            {
               if (hTarget == null)
               {
                  hTarget = new HTextFlowTarget(textFlow, locale);
                  hTarget.setVersionNum(1);
                  textFlow.getTargets().put(locale, hTarget);
               }
               else
               {
                  // DB trigger will copy old value to history table, if we
                  // change the versionNum
                  hTarget.setVersionNum(hTarget.getVersionNum() + 1);
               }
               // NB we don't touch creationDate
               hTarget.setTextFlowRevision(textFlow.getRevision());
               hTarget.setLastChanged(oldTFT.getLastChanged());
               hTarget.setLastModifiedBy(oldTFT.getLastModifiedBy());
               hTarget.setContent(oldTFT.getContent());
               hTarget.setState(oldTFT.getState());
               HSimpleComment hcomment = hTarget.getComment();
               if (hcomment == null)
               {
                  hcomment = new HSimpleComment();
                  hTarget.setComment(hcomment);
               }
               hcomment.setComment(createComment(oldTFT, projectname, version, document.getDocId()));
               textFlowTargetDAO.makePersistent(hTarget);
               ++copyCount;
            }
         }
         textFlowTargetDAO.flush();
         Transaction.instance().commit();

         log.info("copyTrans: {0} {1} translations for document \"{2}{3}\" ", copyCount, locale.getLocaleId(), document.getPath(), document.getName());
      }
      catch (Exception e)
      {
         log.warn(e, e);
         try
         {
            Transaction.instance().rollback();
         }
         catch (Exception i)
         {
            log.warn(i, i);
         }
      }
   }

}
