/*
 * AskPassManager.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.MessageDisplay.PromptWithOptionResult;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.events.AskPassEvent;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AskPassManager
{
   @Inject
   public AskPassManager(final VCSServerOperations server,
                         EventBus eventBus,
                         final GlobalDisplay globalDisplay,
                         final Satellite satellite,
                         final SatelliteManager satelliteManager)
   {

      eventBus.addHandler(AskPassEvent.TYPE, new AskPassEvent.Handler()
      {
         private boolean handleAskPass(String targetWindow)
         {
            // calculate the current window name
            String window = StringUtil.notNull(satellite.getSatelliteName());
            
            // handle it if the target is us
            if (window.equals(targetWindow))
               return true;
            
            // also handle if we are the main window and the specified
            // satellite doesn't exist
            if (!Satellite.isCurrentWindowSatellite() &&
                !satelliteManager.satelliteWindowExists(targetWindow))
               return true;
            
            // otherwise don't handle
            else
               return false;
         }
         
         @Override
         public void onAskPass(final AskPassEvent e)
         {
            if (!handleAskPass(e.getWindow()))
               return;
            
            askpassPending_ = true;
            
            String prompt = e.getPrompt();
            
            // default to password prompt
            String promptLowerCase = prompt.toLowerCase();
            String title = constants_.passwordTitle();
            int dialogType = MessageDisplay.INPUT_PASSWORD;

            if (promptLowerCase.indexOf("password") != -1)
            {
               // if password is mentioned in prompt, treat as password

               // special case: github no longer supports passwords, only personal access tokens,
               // but still prompts for "password", confusing new git users
               if (promptLowerCase.indexOf("https://") != -1 && promptLowerCase.indexOf("github.com") != -1)
               {
                  title = constants_.patTitle();
                  prompt = prompt.replaceFirst("Password", constants_.patPrompt());
               }
               else
               {
                  title = constants_.passwordTitle();
               }
               dialogType = MessageDisplay.INPUT_PASSWORD;
            }
            else if (promptLowerCase.indexOf("username") != -1)
            {
               // if username is mentioned in prompt, treat as username
               title = constants_.usernameTitle();
               dialogType = MessageDisplay.INPUT_USERNAME;
            }
            
            globalDisplay.promptForTextWithOption(
                  title,
                  prompt,
                  "",
                  dialogType,
                  e.getRememberPasswordPrompt(),
                  rememberByDefault_,
                  new ProgressOperationWithInput<PromptWithOptionResult>()
                  {
                     @Override
                     public void execute(final PromptWithOptionResult result,
                                         final ProgressIndicator indicator)
                     {
                        askpassPending_ = false;
                        
                        rememberByDefault_ = result.extraOption;

                        RSAEncrypt.encrypt_ServerOnly(
                              server,
                              result.input,
                              new RSAEncrypt.ResponseCallback()
                              {
                                 @Override
                                 public void onSuccess(String encryptedData)
                                 {
                                    server.askpassCompleted(
                                     encryptedData,
                                     !StringUtil.isNullOrEmpty(e.getRememberPasswordPrompt())
                                         && result.extraOption,
                                     new VoidServerRequestCallback(indicator));
                                    
                                 }

                                 @Override
                                 public void onFailure(ServerError error)
                                 {
                                    Debug.logError(error);

                                    server.askpassCompleted(
                                       null, false,
                                       new SimpleRequestCallback<>());
                                 }
                              });
                     }
                  },
                  new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        askpassPending_ = false;
                        
                        server.askpassCompleted(
                                           null, false,
                                           new SimpleRequestCallback<>());
                     }
                  });
         }
      });
      
      // if there is an askpass pending when the window closes then send an
      // askpass cancel
      Window.addWindowClosingHandler(new ClosingHandler() {

         @Override
         public void onWindowClosing(ClosingEvent event)
         {
            if (askpassPending_)
            {
               askpassPending_ = false;
               
               server.askpassCompleted(null, 
                                       false,
                                       new SimpleRequestCallback<>());
            }
            
         } 
      });


   }

   
   private boolean rememberByDefault_ = true;
   private boolean askpassPending_ = false;
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}
