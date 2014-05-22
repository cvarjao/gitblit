package com.gitblit.markup;

import javax.servlet.ServletContext;

import com.gitblit.Constants;
import com.gitblit.git.GitRevContext;
import com.gitblit.wicket.MarkupProcessor;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;

public class PageResourceContext {
	private ServletContext servletContext;
	private GitRevContext revisionContext;
	private String path;
	private String extension;
	//private String mimeType;
	private String[] encodings;
	private byte[] content;
	private String contentType;
	private MarkupProcessor processor;
	
	public PageResourceContext(ServletContext context, MarkupProcessor processor, GitRevContext revCtx, String path, String[] encodings) {
		this.servletContext=context;
		this.revisionContext=revCtx;
		this.path=path;
		this.encodings=encodings;
		this.processor=processor;
		/*
		this.extension=StringUtils.getFileExtension(path);
		this.mimeType = context.getMimeType(extension);
		if (this.mimeType==null){
			this.mimeType="text/plain";
		}
		*/
	}
	public ServletContext getServletContext() {
		return servletContext;
	}
	public GitRevContext getRevisionContext() {
		return revisionContext;
	}
	public String getPath() {
		return path;
	}
	public String getExtension() {
		return extension;
	}
	public String getMimeType(String targetPath) {
		String newMimeType=servletContext.getMimeType(targetPath);
		if (newMimeType==null){
			newMimeType="text/plain";
		}
		return newMimeType;
	}
	public String getMimeType() {
		return getMimeType(getPath());
	}
	private byte[] fetchBytesContent(String targetPath) {
		return revisionContext.getByteContent(targetPath, false);
	}
	private String fetchStringContent(String targetPath){
		return revisionContext.getStringContent(targetPath, encodings);
	}
	
	public String parseMarkdownContent(String rawContent){
		MarkupDocument markupDoc = processor.parse(this, rawContent);
		return markupDoc.html;
	}
	public boolean fetchResourceContent(String targetPath, boolean evalMarkup){
		boolean ret=false;
		try {
			this.contentType = this.getMimeType(targetPath);
			if (contentType.startsWith("text")) {
				String stringContent=this.fetchStringContent(targetPath);
				if (evalMarkup && this.isMarkupContent()) {
					stringContent = parseMarkdownContent(stringContent);
					contentType="text/html; charset=" + Constants.ENCODING;
				}
				this.content=stringContent.getBytes(Constants.ENCODING);
			} else {
				this.content=this.fetchBytesContent(targetPath);
			}
		} catch (Exception e) {
		}
		return ret;
	}
	
	public boolean fetchResourceContent(){
		return fetchResourceContent(getPath(), true);
	}
	
	private boolean isMarkupContent() {
		return false;
	}
	public void setPath(String path) {
		this.path=path;
	}
	public boolean hasContent() {
		return this.content!=null;
	}
	public byte[] getContent(){
		return content;
	}
	public void setContent(byte[] newContent) {
		this.content=newContent;
	}
	public void setContentType(String newContentType) {
		this.contentType=newContentType;
	}
	public String getContentType() {
		return this.contentType;
	}
}