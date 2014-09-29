package org.baderlab.wordcloud.internal.command;

import org.baderlab.wordcloud.internal.model.SemanticSummaryManager;
import org.baderlab.wordcloud.internal.model.SemanticSummaryParametersFactory;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class CreateWordCloudCommandHandlerTaskFactory implements TaskFactory{
	
	private CyApplicationManager applicationManager;
	private CySwingApplication application;
	private SemanticSummaryManager cloudManager;
	private SemanticSummaryParametersFactory parametersFactory;
	private CreateCloudCommandAction createCloudNoDisplayAction;
	private CyTableManager tableManager;
	private CyTableFactory tableFactory;

	// I'll probably have to specify more (like Network)

	public CreateWordCloudCommandHandlerTaskFactory(CyApplicationManager applicationManager,
			CySwingApplication application, SemanticSummaryManager cloudManager,
			CreateCloudCommandAction createCloudNoDisplayAction, SemanticSummaryParametersFactory parametersFactory, CyTableManager tableManager, CyTableFactory tableFactory) {
		this.applicationManager = applicationManager;
		this.application = application;
		this.cloudManager = cloudManager;
		this.createCloudNoDisplayAction = createCloudNoDisplayAction;
		this.parametersFactory = parametersFactory;
		this.tableManager = tableManager;
		this.tableFactory = tableFactory;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new CreateWordCloudCommandHandlerTask(applicationManager, application, cloudManager, createCloudNoDisplayAction, parametersFactory, tableManager, tableFactory));
	}

	@Override
	public boolean isReady() {
		return true;
	}

}
