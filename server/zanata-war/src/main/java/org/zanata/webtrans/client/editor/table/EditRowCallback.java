package org.zanata.webtrans.client.editor.table;


public interface EditRowCallback
{
   void gotoNextRow();

   void gotoPrevRow();

   void gotoNextFuzzy();

   void gotoPrevFuzzy();
}
