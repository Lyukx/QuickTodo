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
    public void onBindViewHolder(final ViewHolder viewHolder, int i){
        final Todo t = todos.get(i);
        viewHolder.description.setText(t.description);
        viewHolder.time.setText(t.time);

        viewHolder.todoCheck.setText(t.title);
        viewHolder.todoCheck.setChecked(t.done);
        viewHolder.todoCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewHolder.todoCheck.isChecked()){
                    t.done = true;
                }
                else{
                    t.done = false;
                }
            }
        });
    }

    @Override
    public int getItemCount(){
        return todos == null ? 0 : todos.size();
    }

    public void addItem(Todo newTodo){
        // If null
        if(todos == null || todos.size() == 0){
            todos.add(0, newTodo);
        }
        else{
            boolean flag = false;
            for(int i = 0; i < todos.size(); i++){
                if(todos.get(i).time.compareTo(newTodo.time) >= 0){
                    todos.add(i, newTodo);
                    flag = true;
                    break;
                }
            }
            if(!flag){
                todos.add(newTodo);
            }
        }
        notifyDataSetChanged();
    }

    public void removeItem(int deleteNum){
        todos.remove(deleteNum);
        notifyDataSetChanged();
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
