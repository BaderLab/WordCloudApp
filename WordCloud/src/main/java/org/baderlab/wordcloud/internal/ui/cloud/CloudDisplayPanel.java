/*
 File: SemanticSummaryInputPanel.java
 
 Copyright 2010 - The Cytoscape Consortium (www.cytoscape.org)
 
 Code written by: Layla Oesper
 Authors: Layla Oesper, Ruth Isserlin, Daniele Merico
 
 This library is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with this project.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.baderlab.wordcloud.internal.ui.cloud;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.baderlab.wordcloud.internal.model.next.CloudParameters;
import org.baderlab.wordcloud.internal.ui.DualPanelDocker;
import org.baderlab.wordcloud.internal.ui.DualPanelDocker.DockCallback;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

/**
 * The CloudDisplayPanel class defines the panel that displays a Semantic 
 * Summary tag cloud in the South data panel.
 * 
 * @author Layla Oesper
 * @version 1.0
 */
public class CloudDisplayPanel extends JPanel implements CytoPanelComponent
{

	private static final long serialVersionUID = 5996569544692738989L;
	
	private JPanel tagCloudFlowPanel;//add JLabels here for words
	private JScrollPane cloudScroll; 
	private CloudParameters curCloud;
	private JRootPane rootPane;



	public CloudDisplayPanel()
	{
		setLayout(new BorderLayout());
		
		//Create JPanel containing tag words
		tagCloudFlowPanel = initializeTagCloud();
		cloudScroll = new JScrollPane(tagCloudFlowPanel);
		cloudScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		cloudScroll.setBorder(BorderFactory.createEmptyBorder());
		cloudScroll.setBackground(getBackground());
		
		rootPane = new JRootPane(); // use to layer dock button on top
		rootPane.getContentPane().setLayout(new BorderLayout());
		rootPane.getContentPane().add(cloudScroll, BorderLayout.CENTER);
		rootPane.setBackground(getBackground());
		
		add(rootPane, BorderLayout.CENTER);
	}
	
	
	
	public void setDocker(final DualPanelDocker docker) {
		final JButton dockButton = new JButton("Undock");
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttonPanel.add(dockButton);
		buttonPanel.setOpaque(false);
		
		// add some space so the floating button doesn't overlap the scrollbar
		int scrollWidth = ((Integer)UIManager.get("ScrollBar.width")).intValue(); 
		buttonPanel.add(Box.createRigidArea(new Dimension(scrollWidth, 0)));
		
		JPanel glassPane = (JPanel) rootPane.getGlassPane();
		glassPane.setLayout(new BorderLayout());
		glassPane.setVisible(true);
		glassPane.add(buttonPanel, BorderLayout.SOUTH);
		
		
		dockButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				docker.flip();
			}
		});
		
		docker.setCallback(new DockCallback() {
			public void undocked() {
				dockButton.setText("Dock");
			}
			public void docked() {
				dockButton.setText("Undock");
			}
		});
		
	}
	
	
	/**
	 * Initialized a blank tag cloud JPanel object.
	 */
	private JPanel initializeTagCloud()
	{
		JPanel panel = new JPanel(new ModifiedFlowLayout(ModifiedFlowLayout.CENTER,30,25));
		return panel;
	}
	
	/**
	 * Clears all words from the CloudDisplay.
	 */
	public void clearCloud()
	{
		tagCloudFlowPanel.removeAll();
		tagCloudFlowPanel.setLayout(new ModifiedFlowLayout(ModifiedFlowLayout.CENTER, 30, 25));
		tagCloudFlowPanel.revalidate();
		cloudScroll.revalidate();
		tagCloudFlowPanel.updateUI();
		curCloud = null;
		
	}
	
	/**
	 * Updates the tagCloudFlowPanel to include all of the words at the size they
	 * are defined for in params.
	 * @param CloudParameters - parameters of the cloud we want to display.
	 */
	public void updateCloudDisplay(CloudParameters params)
	{
		//clear old info
		this.clearCloud();
		curCloud = params;
		
		//Create a list of the words to include based on MaxWords parameters
		List<CloudWordInfo> copy = new ArrayList<CloudWordInfo>();
		List<CloudWordInfo> original = curCloud.getCloudWordInfoList();
		
		for (int i = 0; i < original.size(); i++)
		{
			CloudWordInfo curInfo = original.get(i);
			copy.add(curInfo);
		}
		Collections.sort(copy);
		
		Integer max = params.getMaxWords();
		Integer numWords = copy.size();
		if (max < numWords)
		{
			copy.subList(max, numWords).clear();
		}
		
		
		//Loop through to create labels and add them
		int count = 0;
		
		Map<Integer,JPanel> clusters = new HashMap<Integer, JPanel>();
		List<CloudWordInfo> wordInfo = curCloud.getCloudWordInfoList();
		Iterator<CloudWordInfo> iter = wordInfo.iterator();
		
		//Loop while more words exist and we are under the max
		while(iter.hasNext() && (count < params.getMaxWords()))
		{
			CloudWordInfo curWordInfo = iter.next();
			
			//Check that word in in our range
			if (copy.contains(curWordInfo))
			{
				Integer clusterNum = curWordInfo.getCluster();
				JLabel curLabel = curWordInfo.createCloudLabel(curCloud);
			
				//Retrieve proper Panel
				JPanel curPanel;
				if (clusters.containsKey(clusterNum))
				{
					curPanel = clusters.get(clusterNum);
				}
				else
				{
					if (params.getDisplayStyle().equals(CloudDisplayStyles.NO_CLUSTERING))
					{
						//curPanel =  new JPanel(new ModifiedFlowLayout(ModifiedFlowLayout.CENTER,10,0));
						curPanel = tagCloudFlowPanel;
						curPanel.setLayout(new ModifiedFlowLayout(ModifiedFlowLayout.CENTER, 10, 0));
					}
					else
					{
					curPanel = new JPanel(new ModifiedClusterFlowLayout(ModifiedFlowLayout.CENTER,10,0));
					}
					
					if (params.getDisplayStyle().equals(CloudDisplayStyles.CLUSTERED_BOXES))
					{
						curPanel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(Color.GRAY), new EmptyBorder(10,10,10,10)));
					}
				}
			
				curPanel.add(curLabel);
				clusters.put(clusterNum, curPanel);
				count++;
			}
		}
		
		//Add all clusters to flow panel
		SortedSet<Integer> sortedSet = new TreeSet<Integer>(clusters.keySet());
		
		for(Iterator<Integer> iter2 = sortedSet.iterator(); iter2.hasNext();)
		{
			Integer clusterNum = iter2.next();
			JPanel curPanel = clusters.get(clusterNum);
			
			if (!curPanel.equals(tagCloudFlowPanel))
				tagCloudFlowPanel.add(curPanel);
		}
		
		tagCloudFlowPanel.revalidate();
		this.revalidate();
		this.updateUI();
		this.repaint();
	}
	

	
	public CloudParameters getCloudParameters()
	{
		return curCloud;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.SOUTH;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return "WordCloud Display";
	}
	

}
