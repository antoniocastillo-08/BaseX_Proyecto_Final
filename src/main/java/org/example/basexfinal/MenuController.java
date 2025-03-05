package org.example.basexfinal;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import org.basex.api.client.ClientSession;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private ComboBox<String> comboBoxColecciones;

    @FXML
    public void initialize() {
        cargarLibrosDesdeBaseX(); // Para cargar los libros al iniciar
        cargarColecciones(); // Para cargar las colecciones al iniciar
        comboBoxColecciones.setOnAction(event -> cargarLibrosDesdeBaseX()); // Actualizar la lista al seleccionar una colección
        btnAñadirReg.setOnAction(event -> añadirRegistro());
        btnEliminarReg.setOnAction(event -> eliminarRegistro());
        btnModificarReg.setOnAction(event -> modificarRegistro());
        btnAñadirCol.setOnAction(event -> añadirColeccion());
        btnEliminarCol.setOnAction(event -> eliminarColeccion());
    }


    private void cargarColecciones() {
        try (ClientSession session = new ClientSession("localhost", 1984, "Castillo", "211808")) {
            session.execute("OPEN library");
            String result = session.execute("LIST library");

            // Filtrar las colecciones correctamente
            List<String> colecciones = Arrays.stream(result.split("\n"))
                    .filter(line -> line.contains("xml")) // Solo líneas que contienen "xml" (colecciones)
                    .map(line -> line.split("\\s+")[0])  // Separar por espacios y tomar la primera palabra
                    .filter(name -> !name.isEmpty()) // Filtrar nombres vacíos
                    .collect(Collectors.toList());

            // Comprobar si hay colecciones encontradas
            if (colecciones.isEmpty()) {
                mostrarAlerta("Error", "No se encontraron colecciones en la base de datos.");
            }

            // Actualizar el ComboBox en el hilo de la interfaz de usuario
            Platform.runLater(() -> comboBoxColecciones.getItems().setAll(colecciones));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cargarLibrosDesdeBaseX() {
        String coleccionSeleccionada = comboBoxColecciones.getValue();
        if (coleccionSeleccionada == null) {
            mostrarAlerta("Error", "Debes seleccionar una colección primero.");
            return;
        }

        try (ClientSession session = new ClientSession("localhost", 1984, "Castillo", "211808")) {
            session.execute("OPEN library");

            String query;

            // Si la colección seleccionada es "books", consulta todos los libros
            if ("books".equals(coleccionSeleccionada)) {
                query = """
                for $book in //books/book
                return concat($book/title, ' - ', $book/author)
            """;
            }
            // Si la colección seleccionada es "fantasy", consulta los libros en esa colección
            else if ("fantasy".equals(coleccionSeleccionada)) {
                query = """
                for $book in //fantasy/book
                return concat($book/title, ' - ', $book/author)
            """;
            } else {
                mostrarAlerta("Error", "Colección no válida.");
                return;
            }

            // Ejecutar la consulta
            String result = session.execute("XQUERY " + query);
            System.out.println("Libros obtenidos:\n" + result);

            // Verificar si hay libros obtenidos
            if (result.isEmpty()) {
                mostrarAlerta("Sin resultados", "No se encontraron libros en la colección seleccionada.");
                return;
            }

            // Convertir los resultados en una lista de registros
            List<String> registros = Arrays.asList(result.split("\n"));

            // Actualizar la interfaz de usuario
            Platform.runLater(() -> listViewRegistros.getItems().setAll(registros));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void añadirRegistro() {
        String coleccionSeleccionada = comboBoxColecciones.getValue();
        if (coleccionSeleccionada == null) {
            mostrarAlerta("Error", "Debes seleccionar una colección primero.");
            return;
        }

        // Crear un diálogo de entrada para que el usuario ingrese los datos del libro
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Añadir Libro");
        dialog.setHeaderText("Introduce los datos del libro (Título, Autor, Género, Año, Editorial)");
        dialog.setContentText("Formato: Título,Autor,Género,Año,Editorial");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String[] datos = input.split(",");
            if (datos.length == 5) {
                // Crear el XML del libro
                String libroXML = String.format("""
                <book>
                    <title>%s</title>
                    <author>%s</author>
                    <genre>%s</genre>
                    <release_date>%s</release_date>
                    <publisher>%s</publisher>
                </book>
            """, datos[0], datos[1], datos[2], datos[3], datos[4]);

                // Insertar el libro en la colección seleccionada
                String queryColeccionSeleccionada = String.format("""
                insert node %s into /%s
            """, libroXML, coleccionSeleccionada);

                // Insertar el libro también en la colección "books"
                String queryBooks = String.format("""
                insert node %s into /books
            """, libroXML);

                // Ejecutar ambas consultas XQuery
                ejecutarXQuery(queryColeccionSeleccionada);
                ejecutarXQuery(queryBooks);

                cargarLibrosDesdeBaseX(); // Recargar los libros después de añadir el nuevo
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
            if (nombreColeccion.trim().isEmpty()) {
                mostrarAlerta("Error", "El nombre de la colección no puede estar vacío.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar archivo XML");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos XML", "*.xml"));
            File archivoSeleccionado = fileChooser.showOpenDialog(null);

            if (archivoSeleccionado == null) {
                mostrarAlerta("Error", "Debes seleccionar un archivo XML.");
                return;
            }

            String rutaArchivo = archivoSeleccionado.getAbsolutePath().replace("\\", "/");

            try (ClientSession session = new ClientSession("localhost", 1984, "Castillo", "211808")) {
                session.execute("OPEN library");

                // Añadir el archivo XML a la base de datos
                session.execute("ADD TO " + nombreColeccion + " " + rutaArchivo);

                cargarLibrosDesdeBaseX();  // Para cargar los registros actualizados
            } catch (Exception e) {
                e.printStackTrace();
                mostrarAlerta("Error", "Error al añadir la colección o archivo XML.");
            }
        });
        cargarColecciones();
    }


    @FXML
    private void eliminarColeccion() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Eliminar Colección");
        dialog.setHeaderText("Introduce el nombre de la colección a eliminar");
        dialog.setContentText("Nombre de la colección:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombreColeccion -> {
            if (nombreColeccion.trim().isEmpty()) {
                mostrarAlerta("Error", "El nombre de la colección no puede estar vacío.");
                return;
            }

            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Confirmar eliminación");
            confirmacion.setHeaderText("¿Estás seguro de que deseas eliminar la colección '" + nombreColeccion + "'?");
            confirmacion.setContentText("Esta acción no se puede deshacer.");

            Optional<ButtonType> respuesta = confirmacion.showAndWait();
            if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
                try (ClientSession session = new ClientSession("localhost", 1984, "Castillo", "211808")) {
                    session.execute("OPEN library");
                    session.execute("DELETE " + nombreColeccion);

                    mostrarAlerta("Éxito", "La colección '" + nombreColeccion + "' ha sido eliminada.");
                    cargarLibrosDesdeBaseX(); // Recargar registros después de eliminar
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarAlerta("Error", "Error al eliminar la colección.");
                }
            }
        });
        cargarColecciones();
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
