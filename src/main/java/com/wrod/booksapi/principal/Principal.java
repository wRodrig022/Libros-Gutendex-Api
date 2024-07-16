package com.wrod.booksapi.principal;


import com.wrod.booksapi.model.*;
import com.wrod.booksapi.repository.AutorRepository;
import com.wrod.booksapi.service.ConsumoApi;
import com.wrod.booksapi.service.ConvierteDatos;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.query.JSqlParserUtils;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoApi consumoApi = new ConsumoApi();
    private final String URL_BASE = "https://gutendex.com/books/";
    private ConvierteDatos conversor = new ConvierteDatos();
    private AutorRepository repository;


    public Principal(AutorRepository repository) {
        this.repository = repository;
    }

    public void muestraElMenu() {

        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    \n______________________________\n
                    1 - Buscar libro por título
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                    0 - Salir
                    """;

            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    listarAutoresVivos();
                    break;
                case 5:
                    listarLibrosPorIdioma();
                    break;
                case 0:
                    System.out.println("Cerrando aplicación ... \n");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }


    private void buscarLibroPorTitulo() {
        System.out.println("Ingresa el título del libro que desea buscar");
        var titulo = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + "?search=" + titulo.replace(" ", "+").toLowerCase());

        if (json.isEmpty() || !json.contains("\"count\":0,\"next\":null,\"previous\":null,\"results\":[]")) {
            var datos = conversor.obtenerDatos(json, Datos.class);

            Optional<DatosLibro> libroBuscado = datos.libros().stream()
                    .findFirst();

            if (libroBuscado.isPresent()) {
                System.out.println(
                        "\n------------- LIBRO --------------" +
                                "\nTítulo: " + libroBuscado.get().titulo() +
                                "\nAutor: " + libroBuscado.get().autores().stream()
                                .map(a -> a.nombre()).limit(1).collect(Collectors.joining()) +
                                "\nIdioma: " + libroBuscado.get().idiomas().stream()
                                .collect(Collectors.joining()) +
                                "\nNúmero de descargas: " + libroBuscado.get().numeroDeDescargas() +
                                "\n--------------------------------------\n"
                );

                try {
                    List<Libro> libroEncontrado = libroBuscado.stream()
                            .map(a -> new Libro(a))
                            .collect(Collectors.toList());

                    Autor autorAPI = libroBuscado.stream()
                            .flatMap(l -> l.autores().stream()
                                    .map(a -> new Autor(a)))
                            .collect(Collectors.toList()).stream().findFirst().get();

                    Optional<Autor> autorBD = repository.buscarAutorPorNombre(libroBuscado.get().autores().stream()
                            .map(a -> a.nombre())
                            .collect(Collectors.joining()));

                    Optional<Libro> libroOptional = repository.buscarLibroPorNombre(titulo);

                    if (libroOptional.isPresent()) {
                        System.out.println("El libro ya está guardado en la BD.");
                    } else {
                        Autor autor;
                        if (autorBD.isPresent()) {
                            autor = autorBD.get();
                            System.out.println("EL autor ya esta guardado en la BD");
                        } else {
                            autor = autorAPI;
                            repository.save(autor);
                        }
                        autor.setLibros(libroEncontrado);
                        repository.save(autor);
                    }
                } catch (Exception e) {
                    System.out.println("Warning! " + e.getMessage());
                }
            } else {
                System.out.println("Libro no encontrado");
            }
        } else {
            System.out.println("Libro no encontrado");
        }
    }

    private void listarLibrosRegistrados() {
        List<Libro> libros = repository.librosRegistrados();
        libros.forEach(System.out::println);
    }

    private void listarAutoresRegistrados() {
        List<Autor> autor;
        autor = repository.findAll();

        autor.stream()
                .sorted(Comparator.comparing(Autor::getNombre))
                .forEach(System.out::println);
    }

    private void listarAutoresVivos() {
        System.out.println("Ingrese un año para verificar el autor(es) que desea buscar");

        try {
            var fecha = Integer.parseInt(teclado.nextLine());
            List<Autor> autores = repository.listarAutoresVivos(fecha);

            if (!autores.isEmpty()){
                autores.stream()
                        .sorted(Comparator.comparing(Autor::getNombre))
                        .forEach(System.out::println);
            } else {
                System.out.println("Ningún autor vivo encontrado en este año");
            }

        } catch (NumberFormatException e){
            System.out.println("Ingrese un año válido " + e.getMessage());
        }

    }

    private void listarLibrosPorIdioma() {
        var menuIdiomas = """
                Elija una opción
                
                1 - Inglés
                2 - Español
                3 - Francés
                4 - Portugués
                5 - Alemán
                
                0 - Regresar
                """;
        System.out.println(menuIdiomas);

        try {
            var opcionIdioma = Integer.parseInt(teclado.nextLine());

            switch (opcionIdioma) {
                case 1:
                    buscarLibrosPorIdioma("en");
                    break;
                case 2:
                    buscarLibrosPorIdioma("es");
                    break;
                case 3:
                    buscarLibrosPorIdioma("fr");
                    break;
                case 4:
                    buscarLibrosPorIdioma("pt");
                    break;
                case 5:
                    buscarLibrosPorIdioma("de");
                    break;
                case 0:
                    System.out.println("Regresando ...");
                    break;
                default:
                    System.out.println("Ingrese una opción válida");
            }
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
        }
    }

    private void buscarLibrosPorIdioma(String idioma) {
        try {
            Idioma idiomaEnum = Idioma.valueOf(idioma.toUpperCase());
            List<Libro> libros = repository.librosPorIdioma(idiomaEnum);

            if (!libros.isEmpty()){
                libros.stream()
                        .forEach(System.out::println);
            } else {
                System.out.println("No hay libros registrados en ese idioma");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Introduce un idioma válido en el formato especificado");
        }
    }

}