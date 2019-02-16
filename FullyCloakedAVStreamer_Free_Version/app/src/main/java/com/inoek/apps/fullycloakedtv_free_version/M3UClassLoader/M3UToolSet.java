package com.inoek.apps.fullycloakedtv_free_version.M3UClassLoader;

public class M3UToolSet {
	/**
	   * Load a m3u file. The file size is larger, the time spends longer.
	   * 
	   * @param filename
	   *            the m3u filename.
	   * @return the instance of M3UFile.
	   */
	  public static M3UFile load(String filename) {
	    final M3UFile file = new M3UFile();
	    final M3UHandler handler = new M3UHandler() {

	      @Override
	      public void onReadEXTM3U(M3UHead header) {
	        file.setHeader(header);
	      }

	      @Override
	      public void onReadEXTINF(M3UItem item) {
	        file.addItem(item);
	      }
	    };
	    M3UFileParser.getInstance().parse(filename, handler);
	    return file;
	  }
}
