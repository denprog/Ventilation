package denis.ventilation;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public final class StatusGridAdapter extends BaseAdapter
{
    private Context context;
    private ArrayList<ArrayList<String>> statusItems;

    public StatusGridAdapter(Context c)
    {
        context = c;
//
//        MainActivity currentActivity = (MainActivity)context;

        statusItems = new ArrayList<ArrayList<String>>();

        ArrayList<String> items = new ArrayList<String>();
        items.add("Связь");
        items.add("Нет");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Состояние");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Режимы");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Кабинет Вентилятор 1");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Кабинет Вентилятор 2");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Спальня Вентилятор 1");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Спальня Вентилятор 2");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Температура 1");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Температура 2");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Нагреватель 1");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Нагреватель 2");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Заслонка Приток");
        items.add("");
        statusItems.add(items);

        items = new ArrayList<String>();
        items.add("Заслонка Вытяжка");
        items.add("");
        statusItems.add(items);
    }

    @Override
    public int getCount()
    {
        return statusItems.size() * 2;
    }

    @Override
    public Object getItem(int i)
    {
        return statusItems.get(i / 2).get(i % 2);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        TextView v;
        if (view == null)
            v = new TextView(context);
        else
            v = (TextView)view;

        v.setTextSize(14);
        v.setText(statusItems.get(i / 2).get(i % 2));

        return v;
    }

    public void setItem(int i, int j, String text)
    {
        statusItems.get(i).set(j, text);
        notifyDataSetChanged();
    }

    public void reset()
    {
        for (ArrayList<String> item : statusItems)
        {
            item.set(1, "");
        }
    }
};
