package jabot.comcon;

import jabot.comcon.config.Config;
import jabot.fileapi.FileApi;
import jabot.idxapi.Index;
import jabot.idxsolr.SolrIndexManager;
import jabot.jabotmodel.Metafiler;
import jabot.jindex.CompositeJindex;
import jabot.metapi.MetadataExtractor;
import jabot.rsapi.Retroshare;
import jabot.taskapi.Tasker;
import jabot.taskri.store.TaskStore;


/**
 * Repositary of all runtime services
 *
 */
public class ServiceCore {
	private Config config;
	private Retroshare retroshare;
	private CmdExecutor commandExecutor;
	private TaskStore taskStore;
	private Tasker tasker;
	private SolrIndexManager solrManager;
	private Index index;
	private CompositeJindex jindex;
	private MetadataExtractor metadataExtractor;
	private Metafiler metafiler;
	private FileApi fileApi;
	
	/** system shutdown requested from console */
	private volatile boolean stopRequested;

	public Retroshare getRetroshare() {
		return retroshare;
	}

	public void setRetroshare(Retroshare retroshare) {
		this.retroshare = retroshare;
	}
	
	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public CmdExecutor getCommandExecutor() {
		return commandExecutor;
	}

	public void setCommandExecutor(CmdExecutor commandExecutor) {
		this.commandExecutor = commandExecutor;
	}
	
	public TaskStore getTaskStore() {
		return taskStore;
	}

	public void setTaskStore(TaskStore taskStore) {
		this.taskStore = taskStore;
	}

	public Tasker getTasker() {
		return tasker;
	}

	public void setTasker(Tasker tasker) {
		this.tasker = tasker;
	}

	public SolrIndexManager getSolrManager() {
		return solrManager;
	}

	public void setSolrManager(SolrIndexManager solrManager) {
		this.solrManager = solrManager;
	}

	public Index getIndex() {
		return index;
	}

	public void setIndex(Index index) {
		this.index = index;
	}

	public CompositeJindex getJindex() {
		return jindex;
	}

	public void setJindex(CompositeJindex jindex) {
		this.jindex = jindex;
	}
	
	public MetadataExtractor getMetadataExtractor() {
		return metadataExtractor;
	}

	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		this.metadataExtractor = metadataExtractor;
	}
	
	public Metafiler getMetafiler() {
		return metafiler;
	}

	public void setMetafiler(Metafiler metafiler) {
		this.metafiler = metafiler;
	}

	public FileApi getFileApi() {
		return fileApi;
	}

	public void setFileApi(FileApi fileApi) {
		this.fileApi = fileApi;
	}

	public boolean isStopRequested() {
		return stopRequested;
	}

	public void setStopRequested(boolean stopRequested) {
		this.stopRequested = stopRequested;
	}
	
}
