package com.example.plandetable;

import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private List<Button> selectedCells = new ArrayList<>();
    private List<int[]> selectedCellPositions = new ArrayList<>();
    private List<Table> tables = new ArrayList<>();  // Liste des tables créées
    private List<Integer> availableTableNumbers = new ArrayList<>();  // Liste des numéros de table disponibles
    private int nextTableNumber = 1;  // Pour attribuer un numéro unique à chaque table
    private static final int GRID_ROWS = 16;
    private static final int GRID_COLS = 24;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vérifier les permissions de stockage
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }*/
        GridView gridView = findViewById(R.id.gridView);

        // Configurer l'adaptateur pour le GridView (Adapter non inclus dans cet exemple)
        CellAdapter adapter = new CellAdapter(this, GRID_ROWS, GRID_COLS, selectedCellPositions, selectedCells);
        gridView.setAdapter(adapter);

        // Bouton personnalisé pour générer le PDF
        Button generatePdfButton = findViewById(R.id.generatePdfButton);
        generatePdfButton.setOnClickListener(v -> generatePdf());

        // Bouton personnalisé
        FrameLayout addButton = findViewById(R.id.addTableButton);
        ImageView iconAdd = findViewById(R.id.icon_add);

        // Ajouter un écouteur pour la réaction à l'appui
        addButton.setOnClickListener(v -> {
            // Appliquer une animation d'échelle pour l'effet de clic
            animateButton(addButton);

            // Appeler la méthode pour ajouter une table après l'animation
            addTable();
        });
    }

    // Méthode pour animer le bouton lors de l'appui
    private void animateButton(View view) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1f, 1.1f,   // Passer de 100% à 110% en largeur
                1f, 1.1f,   // Passer de 100% à 110% en hauteur
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(150);  // Durée de l'animation
        scaleAnimation.setFillAfter(true);  // Garder l'état final de l'animation

        // Réduire à la taille normale après l'agrandissement
        scaleAnimation.setRepeatMode(ScaleAnimation.REVERSE);
        scaleAnimation.setRepeatCount(1);

        // Lancer l'animation
        view.startAnimation(scaleAnimation);
    }

    void onCellClick(Button cell, int row, int col) {
        int[] selectedCell = new int[]{row, col};

        // Si la cellule appartient déjà à une table, ouvrir les options de cette table
        for (Table table : tables) {
            if (table.containsButton(cell)) {
                showTableOptionsDialog(table);
                return;
            }
        }

        // Si la cellule n'est pas dans une table, continuer avec la logique normale
        if (isCellSelected(row, col)) {
            selectedCells.remove(cell);
            removeSelectedPosition(row, col);
            cell.setBackgroundResource(R.drawable.border_grid_btn);  // Réinitialiser la couleur
            cell.setText("");  // Effacer le texte (laisser la cellule vide)
        } else {
            selectedCells.add(cell);
            selectedCellPositions.add(selectedCell);
            cell.setBackgroundColor(Color.BLUE);  // Marquer les cellules sélectionnées
        }
    }

    private void addTable() {
        if (selectedCells.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner des cellules", Toast.LENGTH_SHORT).show();
            return;
        }

        // Générer une couleur unique pour la nouvelle table
        int tableColor = generateUniqueColor();

        // Attribuer un numéro à la nouvelle table en réutilisant un numéro disponible ou en utilisant le prochain numéro
        int tableNumber = getNextTableNumber();

        // Créer une nouvelle table avec les cellules sélectionnées
        Table newTable = new Table(new ArrayList<>(selectedCells), tableColor, tableNumber);
        tables.add(newTable);

        // Changer la couleur des cellules sélectionnées pour représenter la table
        for (Button cell : selectedCells) {
            cell.setBackgroundColor(tableColor);  // Appliquer une couleur unique à chaque table
            cell.setText(String.valueOf(tableNumber));  // Afficher uniquement le numéro de la table
            cell.setTextColor(Color.BLACK);  // Afficher le numéro en noir
        }

        // Réinitialiser la sélection après l'ajout de la table
        selectedCells.clear();
        selectedCellPositions.clear();
    }

    // Méthode pour afficher un dialogue avec les options pour une table (Renommer ou Supprimer)
    private void showTableOptionsDialog(Table table) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options de la table");

        // Options disponibles : Renommer ou Supprimer
        builder.setItems(new CharSequence[]{"Renommer", "Supprimer"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    renameTable(table);  // Appel de la méthode de renommage
                    break;
                case 1:
                    deleteTable(table);  // Appel de la méthode de suppression
                    break;
            }
        });

        builder.show();  // Afficher le dialogue
    }

    // Méthode pour supprimer une table
    private void deleteTable(Table table) {
        // Liste temporaire pour stocker les positions avant de réinitialiser les cellules
        List<int[]> cellPositions = new ArrayList<>();

        // Restaurer la couleur des cellules à leur état initial et les rendre réutilisables
        for (Button cell : table.getTableCells()) {
            // Obtenir les positions avant d'effacer le texte, si les positions sont valides
            //int[] pos = getPositionFromButton(cell);
            //if (pos != null) {
                //cellPositions.add(pos);
                cell.setBackgroundResource(R.drawable.border_grid_btn);  // Réinitialiser la couleur
                cell.setText("");  // Remettre la cellule vide
            //}

            // S'assurer que ces cellules ne sont plus considérées comme sélectionnées ou utilisées
            if (selectedCells.contains(cell)) {
                selectedCells.remove(cell);
            }
        }

        // Supprimer les positions de la liste des positions sélectionnées
        for (int[] pos : cellPositions) {
            removeSelectedPosition(pos[0], pos[1]);
        }

        // Retirer la table de la liste
        tables.remove(table);

        // Rendre le numéro de la table disponible à nouveau
        availableTableNumbers.add(table.getTableNumber());
        availableTableNumbers.sort(Integer::compareTo);  // Garder les numéros triés

        Toast.makeText(this, "Table supprimée", Toast.LENGTH_SHORT).show();
    }

    // Méthode pour obtenir le prochain numéro de table à utiliser
    private int getNextTableNumber() {
        // Si des numéros de table ont été libérés (après suppression), réutiliser le plus petit disponible
        if (!availableTableNumbers.isEmpty()) {
            return availableTableNumbers.remove(0);  // Récupérer le numéro disponible le plus bas
        } else {
            // Si aucun numéro de table n'est disponible, utiliser le prochain numéro dans l'ordre
            return nextTableNumber++;
        }
    }

    // Méthode placeholder pour renommer une table (peut être améliorée selon les besoins)
    private void renameTable(Table table) {
        // Placeholder pour le renommage : on affiche un Toast temporairement
        Toast.makeText(this, "Fonction de renommage non implémentée", Toast.LENGTH_SHORT).show();
    }

    // Supprimer la position sélectionnée pour la désélection
    private void removeSelectedPosition(int row, int col) {
        for (int i = 0; i < selectedCellPositions.size(); i++) {
            int[] selectedCell = selectedCellPositions.get(i);
            if (selectedCell[0] == row && selectedCell[1] == col) {
                selectedCellPositions.remove(i);
                break;
            }
        }
    }

    // Vérifier si une cellule est déjà sélectionnée
    private boolean isCellSelected(int row, int col) {
        for (int[] selectedCell : selectedCellPositions) {
            if (selectedCell[0] == row && selectedCell[1] == col) {
                return true;
            }
        }
        return false;
    }

    // Méthode pour obtenir les coordonnées d'une cellule à partir de son texte
    // Si le texte est vide, retourner null
    private int[] getPositionFromButton(Button cell) {
        String text = cell.getTag().toString();
        if (text.isEmpty()) {
            return null;  // Retourner null si le texte est vide
        }

        // Ne pas récupérer de coordonnées, les boutons ne contiennent plus les "row,col"
        return null;
    }

    // Générer une couleur unique et claire pour chaque table en évitant les teintes de gris
    private int generateUniqueColor() {
        Random random = new Random();
        int color;
        do {
            // Générer des composantes RGB
            int r = random.nextInt(256);
            int g = random.nextInt(256);
            int b = random.nextInt(256);

            // Calculer la luminosité pour s'assurer que la couleur est claire
            double brightness = (0.299 * r) + (0.587 * g) + (0.114 * b);

            // Exclure les teintes de gris (où r, g, et b sont proches)
            if (Math.abs(r - g) > 10 || Math.abs(g - b) > 10 || Math.abs(b - r) > 10) {
                // Si la couleur est suffisamment claire (luminosité > 186), l'accepter
                if (brightness > 186) {
                    color = Color.rgb(r, g, b);
                } else {
                    color = -1;  // Sinon, on rejette la couleur sombre
                }
            } else {
                // Rejeter les teintes de gris
                color = -1;
            }
        } while (color == -1 || tableAlreadyUsesColor(color));  // S'assurer que la couleur est unique

        return color;
    }


    // Vérifier si une couleur est déjà utilisée par une autre table
    private boolean tableAlreadyUsesColor(int color) {
        for (Table table : tables) {
            if (table.getTableColor() == color) {
                return true;
            }
        }
        return false;
    }
    private void generatePdf() {
        try {
            // Créer un document PDF
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // Taille A4
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Définir des paramètres de peinture pour le texte et les cellules
            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(24);
            titlePaint.setTextAlign(Paint.Align.CENTER);

            Paint subtitlePaint = new Paint();
            subtitlePaint.setColor(Color.BLACK);
            subtitlePaint.setTextSize(14);
            subtitlePaint.setTextAlign(Paint.Align.CENTER);

            Paint cellPaint = new Paint();
            cellPaint.setStyle(Paint.Style.STROKE);
            cellPaint.setColor(Color.BLACK);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(12);

            int startX = 50;
            int startY = 150;
            int cellSize = 20;

            // Ajouter le titre au centre du document
            canvas.drawText("Plan de table", pageInfo.getPageWidth() / 2, 50, titlePaint);

            // Ajouter la date et l'heure de génération sous le titre
            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            canvas.drawText(dateTime, pageInfo.getPageWidth() / 2, 80, subtitlePaint);

            // Dessiner les cellules du plan de table
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int x = startX + col * cellSize;
                    int y = startY + row * cellSize;

                    boolean cellFilled = false;

                    // Parcourir toutes les tables pour voir si la cellule actuelle en fait partie
                    for (Table table : tables) {
                        if (table.containsCell(row, col)) {
                            // Remplir la cellule avec la couleur de la table
                            cellPaint.setStyle(Paint.Style.FILL);
                            cellPaint.setColor(table.getTableColor());
                            canvas.drawRect(x, y, x + cellSize, y + cellSize, cellPaint);

                            // Redessiner la bordure noire de la cellule
                            cellPaint.setStyle(Paint.Style.STROKE);
                            cellPaint.setColor(Color.BLACK);
                            canvas.drawRect(x, y, x + cellSize, y + cellSize, cellPaint);

                            // Afficher le numéro de la table dans chaque cellule
                            canvas.drawText(String.valueOf(table.getTableNumber()), x + 5, y + cellSize - 5, textPaint);

                            cellFilled = true;
                            break;
                        }
                    }

                    // Si la cellule n'appartient à aucune table, dessiner seulement la bordure
                    if (!cellFilled) {
                        cellPaint.setStyle(Paint.Style.STROKE);
                        canvas.drawRect(x, y, x + cellSize, y + cellSize, cellPaint);
                    }
                }
            }

            // Terminer la page
            pdfDocument.finishPage(page);

            // Enregistrer le PDF dans un répertoire spécifique à l'application
            File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            // /storage/emulated/0/Android/data/<votre_package>/files/Documents/.
            if (directory != null && !directory.exists()) {
                directory.mkdirs();  // Créer le répertoire si nécessaire
            }

            File file = new File(directory, "PlanDeTable.pdf");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdfDocument.writeTo(fos);
                Toast.makeText(this, "PDF généré avec succès!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erreur lors de la génération du PDF", Toast.LENGTH_SHORT).show();
            }

            // Fermer le document
            pdfDocument.close();
        } catch (Exception e) {
            Log.e("PDFGeneration", "Erreur lors de la génération du PDF", e);
            Toast.makeText(this, "Erreur lors de la génération du PDF", Toast.LENGTH_SHORT).show();
        }
    }

}
