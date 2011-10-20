package org.zanata.webtrans.shared.rpc;

import net.customware.gwt.dispatch.shared.Action;

import org.zanata.common.LocaleId;


public class GetTranslationMemory implements Action<GetTranslationMemoryResult>
{

   public static enum SearchType
   {
      /**
       * Search for exact terms, adjacent to each other and in correct order.
       * (Lucene PhraseQuery)
       */
      EXACT,

      /**
       * Search for similar terms. (Lucene fuzzy query)
       */
      FUZZY,
      /**
       * Uses search string as a raw Lucene query without adding any escapes.
       * Raw
       */
      RAW
   }

   private static final long serialVersionUID = 1L;
   private LocaleId localeId;
   private String query;
   private SearchType searchType;

   @SuppressWarnings("unused")
   private GetTranslationMemory()
   {
   }

   public GetTranslationMemory(String query, LocaleId localeId, SearchType searchType)
   {
      this.query = query;
      this.localeId = localeId;
      this.searchType = searchType;
   }

   public SearchType getSearchType()
   {
      return searchType;
   }

   public void setLocaleId(LocaleId localeId)
   {
      this.localeId = localeId;
   }

   public LocaleId getLocaleId()
   {
      return localeId;
   }

   public void setQuery(String query)
   {
      this.query = query;
   }

   public String getQuery()
   {
      return query;
   }

}
