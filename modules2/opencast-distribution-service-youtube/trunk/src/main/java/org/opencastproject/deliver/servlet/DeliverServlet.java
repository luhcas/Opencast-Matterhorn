/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.deliver.servlet;

import org.opencastproject.deliver.actions.DeliveryAction;
import org.opencastproject.deliver.actions.DeliveryDefault;
import org.opencastproject.deliver.actions.RemoveAction;
import org.opencastproject.deliver.actions.RemoveDefault;
import org.opencastproject.deliver.schedule.Action;
import org.opencastproject.deliver.schedule.InvalidException;
import org.opencastproject.deliver.schedule.Schedule;
import org.opencastproject.deliver.schedule.Task;
import org.opencastproject.deliver.youtube.YouTubeConfiguration;
import org.opencastproject.deliver.youtube.YouTubeDeliveryAction;
import org.opencastproject.deliver.youtube.YouTubeRemoveAction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementing a RESTful (style) web service for requesting
 * media publication.
 *
 * @author Jonathan A. Smith
 */

public class DeliverServlet extends HttpServlet {

    /** Global Scheduler used to schedule delivery actions. */
    private static Schedule schedule;

    /** Logger. */
    private final static Logger LOG = Logger.getLogger("org.opencastproject.deliver");

    /** Test mode -- does not do actual delivery. */
    private static boolean test_mode = false;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String directory_name = config.getInitParameter("data-directory");
        if (directory_name == null || directory_name.equals(""))
            throw new ServletException("Invalid data directory: " + directory_name);

        File data_directory = new File(directory_name);
        data_directory.mkdirs();
        schedule = new Schedule(data_directory);

        // Set test mode
        String testing = config.getInitParameter("testing");
        if (testing != null && testing.trim().equalsIgnoreCase("true")) {
            test_mode = true;
            getServletContext().log("CONFIG: Running in test mode.");
        }

        // Delivery configuration
        String client_id        = config.getInitParameter("client-id"       );
        String developer_key    = config.getInitParameter("developer-key"   );
        String upload_url       = config.getInitParameter("upload-url"      );
        String upload_user      = config.getInitParameter("upload-user"     );
        String upload_password  = config.getInitParameter("upload-password" );
        String category         = config.getInitParameter("default-category");
        String video_private    = config.getInitParameter("private"         );

        YouTubeConfiguration configuration = YouTubeConfiguration.getInstance();
        configuration.setClientId(client_id);
        configuration.setDeveloperKey(developer_key);
        configuration.setUploadUrl(upload_url);
        configuration.setUserId(upload_user);
        configuration.setPassword(upload_password);
        configuration.setCategory(category);
        configuration.setVideoPrivate(video_private == null
                || video_private.trim().equalsIgnoreCase("true"));

        getServletContext().log("CONFIG: " + configuration.toString());
    }

    /**
     * POST request
     *
     * Process a GET request. If the request URL is that of a specific task, 
     * the task is rendered as JSON. If the request is directed to the servlet
     * URL and includes parameters, a new action is created using the
     * parameters and scheduled for execution.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            response.setContentType("text/json");
            PrintWriter out = response.getWriter();
            Action action = makeDeliveryAction(request);
            schedule.start(action);
            out.println(schedule.getSavedTask(action.getName()).toJSON());
            out.close();
        }
        catch (Exception except) {
            reportError(except, response);
        }
    }

    /**
     * DELETE request
     *
     * Process a DELETE request.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */

    @Override
    protected void doDelete(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            response.setContentType("text/json");
            PrintWriter out = response.getWriter();
            Action action = makeRemoveAction(request);
            Task task = schedule.start(action);
            out.println(task.toJSON());
            out.close();
        }
        catch (Exception except) {
            reportError(except, response);
        }
    }

    /**
     * GET request
     *
     * Process a GET request. If the request URL is that of a specific task,
     * the task is rendered as JSON. If the request is directed to the servlet
     * URL and includes parameters, a new action is created using the
     * parameters and scheduled for execution.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) {

        response.setContentType("text/json");
        try {
            String act = request.getParameter("request");
            if (act == null)
                processGet(request, response);
            else if (act.equals("upload"))
                doPost(request, response);
            else if (act.equals("remove"))
                doDelete(request, response);
            else
                processGet(request, response);
        }
        catch (Exception except) {
            reportError(except, response);
        }
    }

    /**
     * Process a GET request that does not specify a 'request' parameter. This
     * is either returns details on a specified request, or if no task name is
     * included in the request path, returns information on all active tasks.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @throws IOException thrown if IO error occurs while writing response
     */

    private void processGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        PrintWriter out = response.getWriter();
        if (path != null && !path.equals("/")) {
            String task_name = path.substring(1);
            Task task = schedule.getSavedTask(task_name);
            if (task != null)
                out.println(task.toJSON());
            else
                out.println("{\"error\": \"Not found\"}");
            out.close();
        }
        else
            reportStatus(response);
    }

    /**
     * Reports on the status of all active tasks.
     *
     * @param response HttpServletResponse
     * @throws IOException thrown if IO error occurs while writing HTML
     */

    private void reportStatus(HttpServletResponse response)
            throws IOException{
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        final String template =
            "<html>"
          + "<head>"
          + "  <title>Delivery Status</title>"
          + "</head>"
          + "<body>"
          + "  <h1>Delivery Status</h1>"
          + "</body>"
          + "</html>";
        out.println(template);
        out.close();
    }

    /**
     * Logs an exception and reports it to the requestor.
     *
     * @param except exception to be reported
     * @param response HttpServletResponse
     */

    private void reportError(Throwable except, HttpServletResponse response) {
        try {
            // Log exception
            LOG.log(Level.SEVERE, "Error occured while processing request", except);

            // Send traceback to requester
            PrintWriter out = response.getWriter();
            response.setContentType("text/json");
            out.print("{\"error\": \"");
            except.printStackTrace(out);
            out.println("\"}");
            out.close();
        }
        catch (IOException except2) {
            LOG.log(Level.SEVERE, "IO Error", except2);
        }
    }

    /**
     * Called when servlet execution is terminating. Calls shutdown method
     * on the schedule object to stop the scheduler's thread pool.
     */
    
    @Override
    public void destroy() {
        schedule.shutdown();
        super.destroy();
    }

    /**
     * Creates and validates an Action for delivering the requested
     * video clip.
     *
     * @param request HttpServletRequest
     * @return delivery action
     */

    private Action makeDeliveryAction(HttpServletRequest request) {

        DeliveryAction action = newDeliveryAction();

        action.setName          (request.getParameter("name"        ));
        action.setDestination   (request.getParameter("destination" ));
        action.setTitle         (request.getParameter("title"       ));
        action.setCreator       (request.getParameter("creator"     ));
        action.setAbstract      (request.getParameter("abstract"    ));
        action.setMediaPath     (request.getParameter("media_path"  ));

        String item_string = request.getParameter("item_number");
        try {
            if (item_string != null)
                action.setItemNumber(Integer.parseInt(item_string));
        }
        catch (NumberFormatException except) {
            throw new InvalidException("Invalid item number: " + item_string,
                    except);
        }

        String tags_string = request.getParameter("tags");
        if (tags_string != null) {
            String[] tags = tags_string.split(",");
            action.setTags(tags);
        }

        action.validate();

        return action;
    }

    /**
     * Returns a new DeliveryAction. Uses the test Action class if servlet
     * is in test mode.
     *
     * @return DeliveryAction
     */

    private DeliveryAction newDeliveryAction() {
        if (test_mode)
            return new DeliveryDefault();
        else
            return new YouTubeDeliveryAction();
    }

    /**
     * Creates and validates an Action for removing the requested
     * video clip.
     *
     * @param request HttpServletRequest
     * @return remove action
     */

    private Action makeRemoveAction(HttpServletRequest request) {
        RemoveAction action;
        if (test_mode)
            action = new RemoveDefault();
        else
            action = new YouTubeRemoveAction();

        String task_name = request.getParameter("name");

        if (task_name == null || task_name.equals("/")) {
            String path = request.getPathInfo();
            task_name = path.substring(1);
        }

        action.setName(task_name + "_r");
        action.setPublishTask(task_name);
        action.validate();

        return action;
    }


}
