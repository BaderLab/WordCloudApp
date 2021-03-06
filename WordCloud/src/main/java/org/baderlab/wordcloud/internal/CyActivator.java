package org.baderlab.wordcloud.internal;
import java.util.Properties;

import javax.swing.Action;

import org.baderlab.wordcloud.internal.command.CreateCloudCommandTask;
import org.baderlab.wordcloud.internal.command.CreateCloudCommandTaskFactory;
import org.baderlab.wordcloud.internal.command.DeleteCloudCommandTaskFactory;
import org.baderlab.wordcloud.internal.command.DelimiterCommandTaskFactory;
import org.baderlab.wordcloud.internal.command.GetVersionCommandTask;
import org.baderlab.wordcloud.internal.command.GetVersionCommandTaskFactory;
import org.baderlab.wordcloud.internal.command.ParentComponentTunableHandlerFactory;
import org.baderlab.wordcloud.internal.command.SelectCloudCommandTaskFactory;
import org.baderlab.wordcloud.internal.command.ShowWordSelectDialogCommand.Type;
import org.baderlab.wordcloud.internal.command.ShowWordSelectDialogCommandFactory;
import org.baderlab.wordcloud.internal.model.CloudModelManager;
import org.baderlab.wordcloud.internal.ui.CloudTaskManager;
import org.baderlab.wordcloud.internal.ui.UIManager;
import org.baderlab.wordcloud.internal.ui.action.CreateCloudAction;
import org.baderlab.wordcloud.internal.ui.action.ExportImageAction;
import org.baderlab.wordcloud.internal.ui.action.ShowAboutDialogAction;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.command.StringTunableHandlerFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;


public class CyActivator extends AbstractCyActivator {
	
	private static final String APPS_MENU = "Apps.WordCloud";
	
	// Semantic Versioning
	// VERY IMPORTANT TO UPDATE FOR EVERY RELEASE!
	public static final Version VERSION = new Version(3,1,4);
	
	
	private CloudTaskManager cloudTaskManager;
	private UIManager uiManager;
	
	@Override
	public void start(BundleContext context) throws Exception {
		
		// Get services
		CyApplicationManager appManager = getService(context, CyApplicationManager.class);
		CySwingApplication application = getService(context, CySwingApplication.class);
		CyTableManager tableManager = getService(context, CyTableManager.class);
		CyTableFactory tableFactory = getService(context, CyTableFactory.class);
		CyNetworkManager networkManager = getService(context, CyNetworkManager.class);
		CyServiceRegistrar registrar = getService(context, CyServiceRegistrar.class);
		FileUtil fileUtil = getService(context, FileUtil.class);
		StreamUtil streamUtil = getService(context, StreamUtil.class);
		OpenBrowser openBrowser = getService(context, OpenBrowser.class);
		
		// Configuration properties
		PropsReader propsReader = new PropsReader("wordcloud", "wordcloud.props");
		Properties propsReaderServiceProps = new Properties();
		propsReaderServiceProps.setProperty("cyPropertyName", "wordcloud.props");
		registerAllServices(context, propsReader, propsReaderServiceProps);

		// Managers
		CloudModelManager cloudModelManager = new CloudModelManager(networkManager, tableManager, streamUtil, propsReader);
		registerAllServices(context, cloudModelManager, new Properties());
		cloudTaskManager = new CloudTaskManager();
		
		uiManager = new UIManager(cloudModelManager, appManager, application, registrar, cloudTaskManager);
		cloudModelManager.addListener(uiManager);
		registerAllServices(context, uiManager, new Properties());
		
		
		// Actions
		AbstractCyAction showAction = uiManager.createShowHideAction();
		showAction.setPreferredMenu(APPS_MENU);
		registerService(context, showAction, CyAction.class, new Properties());
		
		CreateCloudAction createAction = new CreateCloudAction(appManager, application, cloudModelManager, uiManager);
		createAction.setPreferredMenu(APPS_MENU);
		registerService(context, createAction, CyAction.class, new Properties());
		
		ExportImageAction exportImageAction = new ExportImageAction(application, fileUtil, uiManager);
		exportImageAction.setPreferredMenu(APPS_MENU);
		registerService(context, exportImageAction, CyAction.class, new Properties());
		
		Properties props = new Properties();
		props.setProperty(ServiceProperties.TITLE, (String) createAction.getValue(Action.NAME));
		registerService(context, new ActionNodeViewTaskFactory(createAction), NodeViewTaskFactory.class, props);
		
		AbstractCyAction aboutAction = new ShowAboutDialogAction(application, openBrowser);
		aboutAction.setPreferredMenu(APPS_MENU);
		registerService(context, aboutAction, CyAction.class, new Properties());
		
		
		// Session persistence
		SessionListener sessionListener = new SessionListener(cloudModelManager, new IoUtil(streamUtil), networkManager, appManager, uiManager);
		registerAllServices(context, sessionListener, new Properties());
		
		
		// Command line
		registerCommand(context, "create", new CreateCloudCommandTaskFactory(appManager, application, cloudModelManager, uiManager, tableManager, tableFactory), CreateCloudCommandTask.getDescription());
		registerCommand(context, "delete", new DeleteCloudCommandTaskFactory(uiManager), "Deletes a cloud");
		registerCommand(context, "select", new SelectCloudCommandTaskFactory(uiManager), "Selects the nodes that are associated with the cloud");
		registerCommand(context, "version", new GetVersionCommandTaskFactory(), GetVersionCommandTask.getDescription());
		
		registerCommand(context, "delimiter add", new DelimiterCommandTaskFactory(cloudModelManager, uiManager, appManager, true, true), "Adds a delimiter");
		registerCommand(context, "delimiter remove", new DelimiterCommandTaskFactory(cloudModelManager, uiManager, appManager, false, true), "Removes a delimiter");
		registerCommand(context, "ignore add", new DelimiterCommandTaskFactory(cloudModelManager, uiManager, appManager, true, false), "Adds a word that will be ignored");
		registerCommand(context, "ignore remove", new DelimiterCommandTaskFactory(cloudModelManager, uiManager, appManager, false, false), "Removes a word that will be ignored");
		
		ShowWordSelectDialogCommandFactory factory1 = new ShowWordSelectDialogCommandFactory(Type.DELIMITERS, cloudModelManager, uiManager, application, appManager);
		registerCommand(context, "delimiter show", factory1, "Shows the delimiters dialog. Warning: the user must dismiss the dialog.");
		ShowWordSelectDialogCommandFactory factory2 = new ShowWordSelectDialogCommandFactory(Type.WORDS, cloudModelManager, uiManager, application, appManager);
		registerCommand(context, "ignore show", factory2, "Shows the word ignore dialog. Warning: the user must dismiss the dialog.");
		
		registerService(context, new ParentComponentTunableHandlerFactory(), StringTunableHandlerFactory.class, new Properties());
		
		// Always show WordCloud panels when Cytoscape starts.
		//showAction.actionPerformed(null);
	}
	
	
	class PropsReader extends AbstractConfigDirPropsReader {
		public PropsReader(String name, String fileName) {
			super(name, fileName, CyProperty.SavePolicy.CONFIG_DIR);
		}
	}
	
	@Override
	public void shutDown() {
		uiManager.dispose();
		cloudTaskManager.disposeAll();
	}
	
	
	private void registerCommand(BundleContext context, String name, TaskFactory factory, String description) {
		Properties props = new Properties();
		props.put(ServiceProperties.COMMAND, name);
		props.put(ServiceProperties.COMMAND_NAMESPACE, "wordcloud");
		if(description != null)
			props.put(ServiceProperties.COMMAND_DESCRIPTION, description);
		registerService(context, factory, TaskFactory.class, props);
	}
	
	
}
