package com.inoek.apps.fullycloakedtv_free_version.M3UClassLoader;
import java.util.LinkedList;

public class M3UFile {
	  private M3UHead mHeader;
	  private LinkedList<M3UItem> mItems;

	  protected M3UFile() {
	    mItems = new LinkedList<M3UItem>();
	  }

	  public void setHeader(M3UHead header) {
	    mHeader = header;
	  }

	  public M3UHead getHeader() {
	    return mHeader;
	  }

	  public boolean addItem(M3UItem item) {
	    return mItems.add(item);
	  }

	  public boolean addItems(LinkedList<M3UItem> items) {
	    return mItems.addAll(items);
	  }

	  public LinkedList<M3UItem> getItems() {
	    return mItems;
	  }

	  @Override
	  public String toString() {
	    StringBuffer sb = new StringBuffer();
	    if (mHeader != null) {
	      sb.append(mHeader.toString());
	    } else {
	      sb.append("No header");
	    }
	    sb.append('\n');
	    for (M3UItem item : mItems) {
	      sb.append(item.toString());
	      sb.append('\n');
	    }
	    return sb.toString();
	  }
}
