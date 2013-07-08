package com.cognifide.activemq.blob;

import org.apache.activemq.blob.BlobDownloadStrategy;
import org.apache.activemq.blob.BlobTransferPolicy;
import org.apache.activemq.blob.BlobUploadStrategy;

public class SlingBlobTransferPolicy extends BlobTransferPolicy {

	private final SlingBlobServlet blobServlet;

	public SlingBlobTransferPolicy(SlingBlobServlet blobServlet) {
		this.blobServlet = blobServlet;
	}

	@Override
	public SlingBlobTransferPolicy copy() {
		SlingBlobTransferPolicy that = new SlingBlobTransferPolicy(blobServlet);
		that.setDefaultUploadUrl(this.getDefaultUploadUrl());
		that.setBrokerUploadUrl(this.getBrokerUploadUrl());
		that.setUploadUrl(this.getUploadUrl());
		that.setBufferSize(this.getBufferSize());
		return that;
	}

	@Override
	protected BlobUploadStrategy createUploadStrategy() {
		BlobUploadStrategy uploadStrategy = super.createUploadStrategy();
		return new SlingBlobStrategy(uploadStrategy, blobServlet);
	}

	@Override
	protected BlobDownloadStrategy createDownloadStrategy() {
		BlobDownloadStrategy downloadStrategy = super.createDownloadStrategy();
		return new SlingBlobStrategy(downloadStrategy, blobServlet);
	}
}
