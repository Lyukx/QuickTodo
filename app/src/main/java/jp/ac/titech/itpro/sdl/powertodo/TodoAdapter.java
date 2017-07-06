package jp.ac.titech.itpro.sdl.powertodo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

/**
 * Created by lyukaixie on 2017/07/06.
 */

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder>{
    private List<Todo> todos;
    private Context context;

    public TodoAdapter(Context context, List<Todo> todos){
        this.todos = todos;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i){
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.todo_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i){
        Todo t = todos.get(i);
        viewHolder.todoCheck.setText(t.title);
        viewHolder.description.setText(t.description);
        viewHolder.time.setText(t.time);
    }

    @Override
    public int getItemCount(){
        return todos == null ? 0 : todos.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder{
        public CheckBox todoCheck;
        public TextView description;
        public TextView time;

        public ViewHolder(View v){
            super(v);
            todoCheck = (CheckBox) v.findViewById(R.id.todoCheck);
            description = (TextView) v.findViewById(R.id.description);
            time = (TextView) v.findViewById(R.id.time);
        }
    }
}
