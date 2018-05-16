package it.upo.reti2;

import ai.api.model.AIResponse;

import com.google.gson.Gson;

import java.util.List;

import ai.api.GsonFactory;
import ai.api.model.Fulfillment;

import com.vdurmont.emoji.EmojiParser;

import static spark.Spark.*;

/**
 * Dialogflow Webhook example.
 * It gets all tasks, a specified task from a database and provides the information to
 * the Dialogflow service. It also allows users to create a new task, through the Dialogflow
 * conversational interface.
 *
 * @author <a href="mailto:luigi.derussis@uniupo.it">Luigi De Russis</a>
 * @version 1.1 (16/05/2018)
 */
public class TaskWebhook {

    public static void main(String[] args) {

        // init gson, from the Dialogflow factory
        Gson gson = GsonFactory.getDefaultFactory().getGson();

        // the path to our webhook
        post("/tasks", (request, response) -> {
            Fulfillment output = new Fulfillment();

            // the "real" stuff happens here
            // please, notice that the webhook request is a superset of the AIResponse class
            // and it *should* be created to tackle the differences
            // here we keep things at the bare minimum
            doWebhook(gson.fromJson(request.body(), AIResponse.class), output);

            response.type("application/json");
            // output is automatically converted in JSON thanks to gson
            return output;
        }, gson::toJson);

    }

    /**
     * The webhook method. It is where the "magic" happens.
     * Please, notice that in this version we ignore the "urgent" field of tasks.
     *
     * @param input  the request body that comes from Dialogflow
     * @param output the @link(Fulfillment) response to be sent to Dialogflow
     */
    private static void doWebhook(AIResponse input, Fulfillment output) {
        TaskDao taskDao = new TaskDao();
        String text = "";

        // three types of actions/intent to be handled
        // 1. get all tasks
        if (input.getResult().getAction().equalsIgnoreCase("allTasks")) {
            List<Task> allTasks = taskDao.getAllTasks();
            text = "Your tasks are:\n";
            for (Task t : allTasks) {
                text = text.concat(t.getId() + ". " + t.getDescription() + "\n");
            }
        }
        // 2. get a specified task
        else if (input.getResult().getAction().equalsIgnoreCase("singleTask")) {
            int id = input.getResult().getIntParameter("ordinal");
            Task t = taskDao.getTask(id);
            if (t != null)
                text = "The task number " + id + " is: " + t.getDescription();
            else
                text = "Sorry, I cannot find the task " + EmojiParser.parseToUnicode(":slightly_frowning:");
        }
        // 3. insert a new task
        else if (input.getResult().getAction().equalsIgnoreCase("newTask")) {
            taskDao.addTask(input.getResult().getStringParameter("any"), 0);
            text = "Done!";
        }

        // prepare the output
        output.setSpeech(text);
        output.setDisplayText(text);
    }

}
