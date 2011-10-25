package org.zanata.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;

@Name("textFlowTargetDAO")
@AutoCreate
@Scope(ScopeType.STATELESS)
public class TextFlowTargetDAO extends AbstractDAOImpl<HTextFlowTarget, Long>
{

   public TextFlowTargetDAO()
   {
      super(HTextFlowTarget.class);
   }

   public TextFlowTargetDAO(Session session)
   {
      super(HTextFlowTarget.class, session);
   }

   /**
    * @param textFlow
    * @param localeId
    * @return
    */
   public HTextFlowTarget getByNaturalId(HTextFlow textFlow, HLocale locale)
   {
      return (HTextFlowTarget) getSession().createCriteria(HTextFlowTarget.class).add(Restrictions.naturalId().set("textFlow", textFlow).set("locale", locale)).setCacheable(true).setComment("TextFlowTargetDAO.getByNaturalId").uniqueResult();
   }

   @SuppressWarnings("unchecked")
   public List<HTextFlowTarget> findAllTranslations(HDocument document, LocaleId localeId)
   {
      // @formatter:off
      return getSession().createQuery(
         "select t from HTextFlowTarget t where " + 
         "t.textFlow.document =:document " +
         "and t.locale.localeId =:localeId " + 
         "and t.state !=:state " + 
         "order by t.textFlow.pos")
            .setParameter("document", document)
            .setParameter("localeId", localeId)
            .setParameter("state", ContentState.New)
            .list();
      // @formatter:on
   }

   @SuppressWarnings("unchecked")
   public List<HTextFlowTarget> findTranslations(HDocument document, LocaleId localeId)
   {
      // @formatter:off
      return getSession().createQuery(
         "select t from HTextFlowTarget t where " + 
         "t.textFlow.document =:document " +
         "and t.locale.localeId =:localeId " + 
         "and t.state !=:state " +
         "and t.textFlow.obsolete=false " + 
         "order by t.textFlow.pos")
            .setParameter("document", document)
            .setParameter("localeId", localeId)
            .setParameter("state", ContentState.New)
            .list();
      // @formatter:on
   }
   
   public HTextFlowTarget findLatestEquivalentTranslation(HTextFlow textFlow, HLocale locale)
   {
      // @formatter:off
      return (HTextFlowTarget) getSession().createQuery(
         "select t from HTextFlowTarget t " +
         "where t.textFlow.resId = :resid " +
         "and t.textFlow.content = :content " +
         "and t.textFlow.document.docId =:docId " +
         "and t.locale = :locale " +
         "and t.state = :state " +
         "order by t.lastChanged desc")
            .setParameter("content", textFlow.getContent())
            .setParameter("docId", textFlow.getDocument().getDocId())
            .setParameter("locale", locale)
            .setParameter("resid", textFlow.getResId())
            .setParameter("state", ContentState.Approved)
            .setMaxResults(1).uniqueResult();
      // @formatter:on
   }
   
   /**
    * Get the most recent approved translation from a list of candidate
    * HTextFlows.
    * 
    * @param candidateFlowIds ids for the HTextFlows to check for approved
    *           translations
    * @param locale check for translations for this locale
    * @return a {@link HTextFlowTarget} representing the most recent approved
    *         translation, null if there are no approved translations.
    */
   public HTextFlowTarget findLatestApprovedTranslationInCandidateFlows(List<Long> candidateFlowIds, HLocale locale)
   {
      // @formatter:off
      return (HTextFlowTarget) getSession().createQuery(
            "select t from HTextFlowTarget t " +
            "where t.locale = :locale " +
            "and t.state = :state " +
            "and t.textFlow.id in (:flowids) " +
            "order by t.lastChanged desc")
               .setParameter("locale", locale)
               .setParameter("state", ContentState.Approved)
               .setParameterList("flowids", candidateFlowIds)
               .setMaxResults(1).uniqueResult();
      // @formatter:on
   }

   /**
    * Filter a list of locales to give only locales for which there are approved
    * translations for the given document.
    * 
    * @param docId
    * @param candidateLocales
    * @return a subset of candidateLocales for which there are 1 or more
    *         approved translations of strings in the given document
    */
   public List<HLocale> filterLocalesWithDocumentTranslations(String docId, List<HLocale> candidateLocales)
   {
      List<HLocale> filteredLocales = new ArrayList<HLocale>(candidateLocales.size());
      for (HLocale loc : candidateLocales)
      {
         //@formatter:off
         Long count = (Long) getSession().createQuery(
               "select count(*) from HTextFlowTarget tft " +
               "where tft.locale=:locale " +
               "and tft.state=:state " +
               "and tft.textFlow.document.docId=:docId " +
               "group by tft.locale")
                  .setParameter("locale", loc)
                  .setParameter("state", ContentState.Approved)
                  .setParameter("docId", docId)
                  .uniqueResult();
         //@formatter:on
         if (count != null && count > 0) // may not need the && count > 0, if
                                         // empty set is always returned for
                                         // locales with not approved
                                         // translations.
         {
            filteredLocales.add(loc);
         }
      }
      return filteredLocales;
   }

}
