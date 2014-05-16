package com.gitblit.markup;

import java.util.List;

import com.gitblit.IStoredSettings;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;

public interface MarkupParser {
	//public void setup(IStoredSettings settings);
	public boolean canHandle(String documentExt,String documentPath);
	/**
	 * Parses the document.
	 *
	 * @param doc
	 * @param repositoryName
	 * @param commitId
	 */
	void parse(final IStoredSettings settings, final MarkupDocument doc, final String repositoryName, final String commitId);
	public MarkupSyntax getSyntax();
	public List<String> getExtensions();
}
