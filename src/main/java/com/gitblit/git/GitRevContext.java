package com.gitblit.git;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;

import com.gitblit.Constants;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.RepositoryManager;
import com.gitblit.models.RefModel;
import com.gitblit.utils.JGitUtils;

public class GitRevContext {
	private Object lock=new Object();
	private RefModel refModel;
	private RevCommit revCommit;
	private RevTree revTree;
	private NamedRepository namedRepository;
	
	public static class NamedRepository{
		private String name;
		private Repository repository;
		
		public NamedRepository(String name, Repository repository) {
			this.name=name;
			this.repository=repository;
		}
		public String getName() {
			return name;
		}
		public Repository getRepository() {
			return repository;
		}
	}
	public static NamedRepository createNamedRepository(IRepositoryManager repositoryManager, String repositoryName, Repository repository){
		return new NamedRepository(repositoryName, repository);
	}
	public GitRevContext(NamedRepository namedRepository, RefModel refModel, RevCommit revCommit, RevTree revTree) {
		this.namedRepository=namedRepository;
		this.revCommit=revCommit;
		this.revTree=revTree;
	}
	public Repository getRepository() {
		return namedRepository.getRepository();
	}
	public String getRepositoryName() {
		return namedRepository.getName();
	}
	public RefModel getRefModel() {
		return refModel;
	}
	public RevCommit getRevCommit() {
		return revCommit;
	}
	public RevTree getRevTree() {
		return revTree;
	}
	public String getStringContent(String res, String[] encodings) {
		return JGitUtils.getStringContent(getRepository(), revTree, res, encodings);
	}
	public byte[] getByteContent(String res, boolean throwError) {
		return JGitUtils.getByteContent(getRepository(), revTree, res, throwError);
	}
}
