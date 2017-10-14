/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle.
 *
 * Dicoogle/dicoogle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.server;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import pt.ua.dicoogle.server.queryretrieve.QueryRetrieve;

import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.core.ServerSettings;
import pt.ua.dicoogle.rGUI.interfaces.controllers.IServices;
import pt.ua.dicoogle.rGUI.server.controllers.Logs;
import pt.ua.dicoogle.server.web.DicoogleWeb;
import pt.ua.dicoogle.taskManager.TaskManager;

/**
 *
 * @author Samuel Campos <samuelcampos@ua.pt>
 */
public class ControlServices implements IServices
{
    private static final Logger logger = LoggerFactory.getLogger(ControlServices.class);
    
    private static ControlServices instance = null;
    // Services vars
    private RSIStorage storage = null;
    private boolean webServicesRunning = false;
    private boolean webServerRunning = false;
    private QueryRetrieve retrieve = null;
    
    private DicoogleWeb webServices;
    
    private ControlServices()
    {
        TaskManager taskManager = new TaskManager(Integer.parseInt(System.getProperty("dicoogle.taskManager.nThreads", "4")));

        startInicialServices();
    }

    public static synchronized ControlServices getInstance()
    {
//      sem.acquire();
        if (instance == null)
        {
            instance = new ControlServices();
        }
//      sem.release();
        return instance;
    }


    /* Strats the inicial services based on ServerSettings */
    private void startInicialServices()
    {
        ServerSettings settings = ServerSettings.getInstance();

        try
        {
          /*  if (settings.isP2P())
            {
                startP2P();
            }*/

            if (settings.isStorage())
            {
                startStorage();
            }

            if (settings.isQueryRetrive())
            {
                startQueryRetrieve();
            }

            if(settings.getWeb().isWebServer()){
            	startWebServer();
            }

            if (settings.getWeb().isWebServices())
            {
                startWebServices();
            }

        } catch (Exception ex)
        {
            logger.error(ex.getMessage(), ex);
        }
    }

    /* Stop all services that are running */
    @Override
    public boolean stopAllServices()
    {
        try
        {
        	//TODO: DELETED
            //PluginController.getInstance().stopAll();
           // stopP2P();
            stopStorage();
            stopQueryRetrieve();
            stopWebServices();
        } catch (Exception ex)
        {
            logger.error(ex.getMessage(), ex);
            return false;
        }

        return true;
    }

    /**
     *
     * @return   0 - if everything is fine and the service was started
     *          -1 - if the server's storage path is not defined
     *          -2 - service is already running
     * 
     * @throws IOException
     */
    @Override
    public int startStorage() throws IOException
    {
        if (storage == null)
        {
            ServerSettings settings = ServerSettings.getInstance();

            SOPList list = SOPList.getInstance();
            //list.setDefaultSettings();

            int i;

            List l = list.getKeys();
            String[] keys = new String[l.size()];

            for (i = 0; i < l.size(); i++)
            {
                keys[i] = (String) l.get(i);
            }
            storage = new RSIStorage(keys, list);
            storage.start();

            //DebugManager.getInstance().debug("Starting DICOM Storage SCP");
            Logs.getInstance().addServerLog("Starting DICOM Storage SCP");

            return 0;
        }

        return -2;
    }

    @Override
    public void stopStorage()
    {
        if (storage != null)
        {
            storage.stop();
            storage = null;
            //DebugManager.getInstance().debug("Stopping DICOM Storage SCP");
            Logs.getInstance().addServerLog("Stopping DICOM Storage SCP");
        }
    }

    @Override
    public boolean storageIsRunning()
    {
        return storage != null;
    }

    @Override
    public void startQueryRetrieve()
    {
        if (retrieve == null)
        {
            retrieve = new QueryRetrieve();
            retrieve.startListening();
            //DebugManager.getInstance().debug("Starting DICOM QueryRetrive");
            Logs.getInstance().addServerLog("Starting DICOM QueryRetrive");
        }
    }

    @Override
    public void stopQueryRetrieve()
    {
        if (retrieve != null)
        {
            retrieve.stopListening();
            retrieve = null;
            //DebugManager.getInstance().debug("Stopping DICOM QueryRetrive");
            Logs.getInstance().addServerLog("Stopping DICOM QueryRetrive");
        }
    }

    @Override
    public boolean queryRetrieveIsRunning()
    {
        return retrieve != null;
    }

    @Override
    public boolean webServerIsRunning()
    {
        return webServerRunning;
    }

    @Override
    @Deprecated
    public void startWebServices()
    {
    }

    @Override
    @Deprecated
    public void stopWebServices()
    {
    }

    @Override
    public boolean webServicesIsRunning()
    {
        return webServicesRunning;
    }
    
    //TODO: Review those below!
    @Override
    public void startWebServer(){
        logger.info("Starting WebServer");

        try {
            if (webServices == null) {
                webServices = new DicoogleWeb(ServerSettings.getInstance().getWeb().getServerPort());
                webServerRunning = true;
                webServicesRunning = true;
                logger.info("Starting Dicoogle Web");
            }
        } catch (Exception ex) {
            logger.error("Failed to launch the web server", ex);
        }
        
    }

    @Override
    public void stopWebServer(){
        logger.info("Stopping Web Server");
        
        if(webServices != null){
            try { 
                webServicesRunning = false;
                webServerRunning = false;
                
                webServices.stop();
                
                webServices = null;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        logger.info("Stopping Dicoogle Web");
    }
    
    public DicoogleWeb getWebServicePlatform(){
    	return webServices;
    }
    
}
