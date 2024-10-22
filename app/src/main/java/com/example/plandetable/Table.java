package com.example.plandetable;

import android.widget.Button;
import java.util.List;

public class Table {
    private List<Button> tableCells; // Liste des boutons qui appartiennent à cette table
    private int tableColor; // La couleur unique de la table
    private int tableNumber; // Le numéro de la table

    public Table(List<Button> tableCells, int tableColor, int tableNumber) {
        this.tableCells = tableCells;
        this.tableColor = tableColor;
        this.tableNumber = tableNumber;
    }

    public List<Button> getTableCells() {
        return tableCells;
    }

    public int getTableColor() {
        return tableColor;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    // Vérifie si un bouton spécifique est contenu dans cette table
    public boolean containsButton(Button button) {
        return tableCells.contains(button);
    }

    // Vérifie si la cellule donnée par ses coordonnées est contenue dans la table
    public boolean containsCell(int row, int col) {
        for (Button button : tableCells) {
            int[] position = getPositionFromButton(button);
            if (position != null && position[0] == row && position[1] == col) {
                return true;
            }
        }
        return false;
    }

    // Détermine si la cellule donnée est la première dans la liste des cellules de la table
    public boolean isFirstCell(int row, int col) {
        if (!tableCells.isEmpty()) {
            Button firstCell = tableCells.get(0);
            int[] position = getPositionFromButton(firstCell);
            return position != null && position[0] == row && position[1] == col;
        }
        return false;
    }

    // Méthode utilitaire pour extraire les coordonnées à partir du bouton
    private int[] getPositionFromButton(Button button) {
        String text = button.getTag().toString();
        if (text.isEmpty() || !text.contains(",")) {
            return null;  // Retourner null si le texte est vide ou incorrect
        }

        String[] pos = text.split(",");
        try {
            return new int[]{Integer.parseInt(pos[0]), Integer.parseInt(pos[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
