package jp.ac.titech.itpro.sdl.powertodo;

import java.util.Date;

/**
 * Created by lyukaixie on 2017/07/03.
 */

public class Todo {
    public String title;
    public String description;
    public String time;
    public boolean done;

    // Create a todoitem with description and date
    public Todo(String title, String description, String time){
        this.title = title;
        this.description = description;
        this.time = time;
        this.done = false;
    }

    // Create a quick todoitem
    public Todo(String title){
        this.title = title;
        this.description = "";
        this.time = "";
        this.done = false;
    }
}
