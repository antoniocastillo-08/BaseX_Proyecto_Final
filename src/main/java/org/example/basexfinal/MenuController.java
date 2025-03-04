package org.example.basexfinal;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import org.basex.api.client.ClientSession;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MenuController {
    @FXML
    private ListView<String> listViewRegistros;
    @FXML
    private Button btnAñadirReg;
    @FXML
    private Button btnEliminarReg;
    @FXML
    private Button btnModificarReg;
    @FXML
    private Button btnEliminarCol;
    @FXML
    private Button btnAñadirCol;
    @FXML
    public void initialize() {
        cargarLibrosDesdeBaseX();
        btnAñadirReg.setOnAction(event->añadirRegistro());
        btnEliminarReg.setOnAction(event->eliminarRegistro());
        btnModificarReg.setOnAction(event->modificarRegistro());
        btnAñadirCol.setOnAction(event->añadirColeccion());
        btnEliminarCol.setOnAction(event->eliminarRegistro());
    }

    @FXML
    private void cargarLibrosDesdeBaseX() {
        try (ClientSession session = new ClientSession("localhost", 1984, "Castillo", "211808")) {
            session.execute("OPEN library");
            String query = """
                for $book in /library/books/book
                return concat($book/title, ' - ', $book/author, ' - ', $book/genre)
            """;
            String result = session.execute("XQUERY " + query);
            System.out.println("Libros obtenidos:\n" + result);

            List<String> registros = Arrays.asList(result.split("\n"));
            Platform.runLater(() -> listViewRegistros.getItems().setAll(registros));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void añadirRegistro() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Añadir Libro");
        dialog.setHeaderText("Introduce los datos del libro (Título, Autor, Género, Año, Editorial)");
        dialog.setContentText("Formato: Título,Autor,Género,Año,Editorial");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String[] datos = input.split(",");
            if (datos.length == 5) {
                String query = String.format("""
                    insert node <book>
                        <title>%s</title>
                        <author>%s</author>
                        <genre>%s</genre>
                        <release_date>%s</release_date>
                        <publisher>%s</publisher>
                    </book> into /library/books
                """, datos[0], datos[1], datos[2], datos[3], datos[4]);

                ejecutarXQuery(query);
                cargarLibrosDesdeBaseX();
            } else {
                mostrarAlerta("Error", "Formato incorrecto. Usa: Título,Autor,Género,Año,Editorial");
            }
        });
    }

    @FXML
    private void eliminarRegistro() {
        String seleccionado = listViewRegistros.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Error", "Selecciona un libro para eliminar.");
            return;
        }

        String titulo = seleccionado.split(" - ")[0];
        String query = String.format("""
            delete node /library/books/book[title='%s']
        """, titulo);

        ejecutarXQuery(query);
        cargarLibrosDesdeBaseX();
    }

    @FXML
    private void modificarRegistro() {
        String seleccionado = listViewRegistros.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Error", "Selecciona un libro para modificar.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Modificar Libro");
        dialog.setHeaderText("Introduce los nuevos datos (Título, Autor, Género, Año, Editorial)");
        dialog.setContentText("Formato: Título,Autor,Género,Año,Editorial");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String[] datos = input.split(",");
            if (datos.length == 5) {
                String tituloViejo = seleccionado.split(" - ")[0];
                String query = String.format("""
                    let $libro := /library/books/book[title='%s']
                    return (
                        replace value of node $libro/title with '%s',
                        replace value of node $libro/author with '%s',
                        replace value of node $libro/genre with '%s',
                        replace value of node $libro/release_date with '%s',
                        replace value of node $libro/publisher with '%s'
                    )
                """, tituloViejo, datos[0], datos[1], datos[2], datos[3], datos[4]);

                ejecutarXQuery(query);
                cargarLibrosDesdeBaseX();
            } else {
                mostrarAlerta("Error", "Formato incorrecto. Usa: Título,Autor,Género,Año,Editorial");
            }
        });
    }
    @FXML
    private void añadirColeccion() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Añadir Colección");
        dialog.setHeaderText("Introduce el nombre de la nueva colección");
        dialog.setContentText("Nombre de la colección:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombreColeccion -> {
            String query = String.format("""
            ADD TO 
        """, nombreColeccion);

            ejecutarXQuery(query);
            mostrarAlerta("Éxito", "Colección '" + nombreColeccion + "' creada.");
        });
    }

    @FXML
    private void eliminarColeccion() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Eliminar Colección");
        dialog.setHeaderText("Introduce el nombre de la colección a eliminar");
        dialog.setContentText("Nombre de la colección:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombreColeccion -> {
            String query = String.format("""
            DROP COLLECTION '%s'
        """, nombreColeccion);

            ejecutarXQuery(query);
            mostrarAlerta("Éxito", "Colección '" + nombreColeccion + "' eliminada.");
        });
    }

    private void ejecutarXQuery(String query) {
        try (ClientSession session = new ClientSession("localhost", 1984, "Castillo", "211808")) {
            session.execute("OPEN library");
            session.execute("XQUERY " + query);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo ejecutar la operación.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}
