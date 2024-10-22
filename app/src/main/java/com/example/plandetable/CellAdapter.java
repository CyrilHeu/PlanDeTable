package com.example.plandetable;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import java.util.List;

public class CellAdapter extends BaseAdapter {

    private Context context;
    private int rowCount;
    private int colCount;
    private List<int[]> selectedCellPositions;
    private List<Button> selectedCells;
    private MainActivity mainActivity;

    public CellAdapter(Context context, int rowCount, int colCount, List<int[]> selectedCellPositions, List<Button> selectedCells) {
        this.context = context;
        this.rowCount = rowCount;
        this.colCount = colCount;
        this.selectedCellPositions = selectedCellPositions;
        this.selectedCells = selectedCells;
        this.mainActivity = (MainActivity) context;
    }

    @Override
    public int getCount() {
        return rowCount * colCount;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int row = position / colCount;
        final int col = position % colCount;

        Button cellButton;
        if (convertView == null) {
            cellButton = new Button(context);
        } else {
            cellButton = (Button) convertView;
        }

        cellButton.setTag(String.format("%d,%d", row, col));
        cellButton.setBackgroundColor(android.graphics.Color.LTGRAY);
        cellButton.setBackgroundResource(R.drawable.border_grid_btn);
        // Lorsqu'on clique sur une cellule, appeler la méthode de MainActivity
        cellButton.setOnClickListener(v -> mainActivity.onCellClick(cellButton, row, col));

        return cellButton;
    }
}
