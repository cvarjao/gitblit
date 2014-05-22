/*
 * Copyright 2013 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.wicket;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.pegdown.DefaultVerbatimSerializer;
import org.pegdown.LinkRenderer;
import org.pegdown.ToHtmlSerializer;
import org.pegdown.VerbatimSerializer;
import org.pegdown.plugins.ToHtmlSerializerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.markup.MarkupParser;
import com.gitblit.markup.PageResourceContext;
import com.gitblit.markup.PlainTextSyntax;
import com.gitblit.markup.RawTextSyntax;
import com.gitblit.models.PathModel;
import com.gitblit.utils.JGitUtils;
import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;
import com.google.common.base.Joiner;

/**
 * Processes markup content and generates html with repository-relative page and
 * image linking.
 *
 * @author James Moger
 *
 */
public class MarkupProcessor {

	public enum MarkupSyntax {
		PLAIN, MARKDOWN, TWIKI, TRACWIKI, TEXTILE, MEDIAWIKI, CONFLUENCE
	}

	private Logger logger = LoggerFactory.getLogger(getClass());

	private final IStoredSettings settings;

	private final GitBlitWebApp app;

	public MarkupProcessor(GitBlitWebApp app) {
		this.app=app;
		this.settings = app.settings();
	}

	public List<String> getMarkupExtensions() {
		List<String> list = new ArrayList<String>();
		for (MarkupParser parser:getAllParsers()){
			list.addAll(parser.getExtensions());
		}
		return list;
	}

	public List<String> getAllExtensions() {
		List<String> list = getMarkupExtensions();
		list.add("txt");
		list.add("TXT");
		return list;
	}

	private List<String> getRoots() {
		return settings.getStrings(Keys.web.documents);
	}

	private String [] getEncodings() {
		return settings.getStrings(Keys.web.blobEncodings).toArray(new String[0]);
	}
	public boolean isMarkupExtension(String ext){
		MarkupParser parser=null;
		try {
			parser=determineParser("file."+ext);
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		return parser==null || !MarkupSyntax.PLAIN.equals(parser.getSyntax());
	}
	private List<MarkupParser> getAllParsers() {
		List<MarkupParser> classes=new ArrayList<>();
		List<String> parsers=settings.getStrings(Keys.web.markupParsers);
		for (String parser:parsers){
			MarkupParser parserObj;
			try {
				Constructor<?> constructor=Class.forName(parser).getConstructor(new Class[]{GitBlitWebApp.class});
				parserObj = (MarkupParser) constructor.newInstance(app);
				classes.add(parserObj);
			} catch (Exception e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
		return classes;
	}
	private MarkupParser determineParser(String documentPath) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		String ext = StringUtils.getFileExtension(documentPath).toLowerCase();
		List<MarkupParser> parsers=getAllParsers();
		
		for (MarkupParser parser:parsers){
			if (parser.canHandle(ext, documentPath)){
				return parser;
			}
		}
		return null;
	}

	public boolean hasRootDocs(Repository r) {
		List<String> roots = getRoots();
		List<String> extensions = getAllExtensions();
		List<PathModel> paths = JGitUtils.getFilesInPath(r, null, null);
		for (PathModel path : paths) {
			if (!path.isTree()) {
				String ext = StringUtils.getFileExtension(path.name).toLowerCase();
				String name = StringUtils.stripFileExtension(path.name).toLowerCase();

				if (roots.contains(name)) {
					if (StringUtils.isEmpty(ext) || extensions.contains(ext)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public List<MarkupDocument> getRootDocs(Repository r, String repositoryName, String commitId) {
		List<String> roots = getRoots();
		List<MarkupDocument> list = getDocs(r, repositoryName, commitId, roots);
		return list;
	}

	public MarkupDocument getReadme(Repository r, String repositoryName, String commitId) {
		List<MarkupDocument> list = getDocs(r, repositoryName, commitId, Arrays.asList("readme"));
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	private List<MarkupDocument> getDocs(Repository r, String repositoryName, String commitId, List<String> names) {
		List<String> extensions = getAllExtensions();
		String [] encodings = getEncodings();
		Map<String, MarkupDocument> map = new HashMap<String, MarkupDocument>();
		RevCommit commit = JGitUtils.getCommit(r, commitId);
		List<PathModel> paths = JGitUtils.getFilesInPath(r, null, commit);
		for (PathModel path : paths) {
			if (!path.isTree()) {
				String ext = StringUtils.getFileExtension(path.name).toLowerCase();
				String name = StringUtils.stripFileExtension(path.name).toLowerCase();

				if (names.contains(name)) {
					if (StringUtils.isEmpty(ext) || extensions.contains(ext)) {
						String markup = JGitUtils.getStringContent(r, commit.getTree(), path.name, encodings);
						MarkupDocument doc = parse(repositoryName, commitId, path.name, markup);
						map.put(name, doc);
					}
				}
			}
		}
		// return document list in requested order
		List<MarkupDocument> list = new ArrayList<MarkupDocument>();
		for (String name : names) {
			if (map.containsKey(name)) {
				list.add(map.get(name));
			}
		}
		return list;
	}

	public MarkupDocument parse(PageResourceContext context, String rawContent) {
		return this.parse(context.getRevisionContext().getRepositoryName(), context.getRevisionContext().getRevCommit().getName(), context.getPath(), rawContent);
	}
	
	public MarkupDocument parse(String repositoryName, String commitId, String documentPath, String markupText) {

		
		MarkupParser defaultSyntax = new RawTextSyntax(app);
		MarkupParser syntax = defaultSyntax;
		try {
			syntax = determineParser(documentPath);
		} catch (InstantiationException e) {
			logger.error(e.getLocalizedMessage(), e);
		} catch (IllegalAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		} catch (ClassNotFoundException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		if (syntax==null){
			syntax=defaultSyntax;
		}
		final MarkupDocument doc = new MarkupDocument(repositoryName, commitId, documentPath, markupText, syntax);
		doc.parse(settings);
		return doc;
	}

	public static class MarkupDocument implements Serializable {

		private static final long serialVersionUID = 1L;

		public final String repositoryName;
		public final String commitId;
		public final String documentPath;
		public final String markupText;
		public final MarkupParser parser;
		public String html;

		MarkupDocument(String repositoryName, String commitId, String documentPath, String markupText, MarkupParser parser) {
			this.repositoryName=repositoryName;
			this.commitId=commitId;
			this.documentPath = documentPath;
			this.markupText = markupText;
			this.parser = parser;
		}
		
		public boolean isPlainText(){
			return MarkupSyntax.PLAIN.equals(getSyntax());
		}
		
		private MarkupSyntax getSyntax(){
			return this.parser.getSyntax();
		}
		public void parse(IStoredSettings settings){
			if (parser!=null) parser.parse(settings, this, repositoryName, commitId);
			if (this.html == null) {
				String newMarkupText=markupText;
				// failed to transform markup
				if (newMarkupText == null) {
					newMarkupText = String.format("Document <b>%1$s</b> not found in <em>%2$s</em>", documentPath, repositoryName);
				}
				newMarkupText = MessageFormat.format("<div class=\"alert alert-error\"><strong>{0}:</strong> {1}</div>{2}", "Error", "failed to parse markup", newMarkupText);
				this.html = StringUtils.breakLinesForHtml(newMarkupText);
			}
		}
		String getCurrentPath() {
			String basePath = "";
			if (documentPath.indexOf('/') > -1) {
				basePath = documentPath.substring(0, documentPath.lastIndexOf('/') + 1);
				if (basePath.charAt(0) == '/') {
					return basePath.substring(1);
				}
			}
			return basePath;
		}

		public String getRelativePath(String ref) {
			if (ref.charAt(0) == '/') {
				// absolute path in repository
				return ref.substring(1);
			} else {
				// resolve relative repository path
				String cp = getCurrentPath();
				if (StringUtils.isEmpty(cp)) {
					return ref;
				}
				// this is a simple relative path resolver
				List<String> currPathStrings = new ArrayList<String>(Arrays.asList(cp.split("/")));
				String file = ref;
				while (file.startsWith("../")) {
					// strip ../ from the file reference
					// drop the last path element
					file = file.substring(3);
					currPathStrings.remove(currPathStrings.size() - 1);
				}
				currPathStrings.add(file);
				String path = Joiner.on("/").join(currPathStrings);
				return path;
			}
		}
	}

	/**
	 * This class implements a workaround for a bug reported in issue-379.
	 * The bug was introduced by my own pegdown pull request #115.
	 *
	 * @author James Moger
	 *
	 */
	public static class WorkaroundHtmlSerializer extends ToHtmlSerializer {

		 public WorkaroundHtmlSerializer(final LinkRenderer linkRenderer) {
			 super(linkRenderer,
					 Collections.<String, VerbatimSerializer>singletonMap(VerbatimSerializer.DEFAULT, DefaultVerbatimSerializer.INSTANCE),
					 Collections.<ToHtmlSerializerPlugin>emptyList());
		    }
	    private void printAttribute(String name, String value) {
	        printer.print(' ').print(name).print('=').print('"').print(value).print('"');
	    }

	    /* Reimplement print image tag to eliminate a trailing double-quote */
		@Override
	    protected void printImageTag(LinkRenderer.Rendering rendering) {
	        printer.print("<img");
	        printAttribute("src", rendering.href);
	        printAttribute("alt", rendering.text);
	        for (LinkRenderer.Attribute attr : rendering.attributes) {
	            printAttribute(attr.name, attr.value);
	        }
	        printer.print("/>");
	    }
	}
}
