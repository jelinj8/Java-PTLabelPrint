package cz.bliksoft.ptlabelprint.protocol.niimbot;

/** Response to {@link RequestCommandId#PRINT_STATUS}. */
public class PrintStatus {

	/** 0-n */
	private final int page;
	/** 0-100 */
	private final int pagePrintProgress;
	/** 0-100 */
	private final int pageFeedProgress;
	private final int error;

	public PrintStatus(int page, int pagePrintProgress, int pageFeedProgress, int error) {
		this.page = page;
		this.pagePrintProgress = pagePrintProgress;
		this.pageFeedProgress = pageFeedProgress;
		this.error = error;
	}

	public int getPage() {
		return page;
	}

	public int getPagePrintProgress() {
		return pagePrintProgress;
	}

	public int getPageFeedProgress() {
		return pageFeedProgress;
	}

	public int getError() {
		return error;
	}
}
