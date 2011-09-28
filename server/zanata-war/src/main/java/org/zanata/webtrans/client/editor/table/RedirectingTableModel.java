package org.zanata.webtrans.client.editor.table;


import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModelHelper.Request;

public class RedirectingTableModel<RowType> extends MutableTableModel<RowType>
{

   private TableModelHandler<RowType> tableModelHandler;

   public RedirectingTableModel()
   {
   }

   public RedirectingTableModel(TableModelHandler<RowType> dataSource)
   {
      this.tableModelHandler = dataSource;
   }

   public void onCancel(RowType cellValue)
   {
      if (tableModelHandler != null)
         tableModelHandler.onCancel(cellValue);
   }

   @Override
   protected boolean onRowInserted(int beforeRow)
   {
      if (tableModelHandler != null)
         return tableModelHandler.onRowInserted(beforeRow);
      return true;
   }

   @Override
   protected boolean onRowRemoved(int row)
   {
      if (tableModelHandler != null)
         return tableModelHandler.onRowRemoved(row);
      return true;
   }

   @Override
   protected boolean onSetRowValue(int row, RowType rowValue)
   {
      if (tableModelHandler != null)
         return tableModelHandler.onSetRowValue(row, rowValue);
      return false;
   }

   @Override
   public void requestRows(final Request request, final Callback<RowType> callback)
   {
      if (tableModelHandler != null)
         tableModelHandler.requestRows(request, callback);
      else
         callback.onFailure(new RuntimeException("No datasource"));
   }

   public void setTableModelHandler(TableModelHandler<RowType> tableModelHandler)
   {
      this.tableModelHandler = tableModelHandler;
   }

   public TableModelHandler<RowType> getTableModelHandler()
   {
      return tableModelHandler;
   }

   public void gotoNextRow()
   {
      if (tableModelHandler != null)
         tableModelHandler.gotoNextRow();
   }

   public void gotoPrevRow()
   {
      if (tableModelHandler != null)
         tableModelHandler.gotoPrevRow();
   }

   public void gotoNextFuzzy()
   {
      if (tableModelHandler != null)
         tableModelHandler.nextFuzzyIndex();
   }

   public void gotoPrevFuzzy()
   {
      if (tableModelHandler != null)
         tableModelHandler.prevFuzzyIndex();
   }
}
