/*
 * Copyright 2017 Clement Levallois
 * http://wwww.clementlevallois.net
 */
package net.clementlevallois.java.repl.jar;

import static com.googlecode.totallylazy.Files.temporaryDirectory;
import com.googlecode.totallylazy.Sequence;
import static com.googlecode.totallylazy.Sequences.sequence;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

import java.io.File;
import java.io.FilePermission;
import java.io.UnsupportedEncodingException;
import static java.lang.System.getProperty;
import java.lang.management.ManagementPermission;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Date;
import java.util.PropertyPermission;
import javarepl.Repl;
import static javarepl.Result.result;
import javarepl.console.ConsoleConfig;
import static javarepl.console.ConsoleConfig.consoleConfig;
import javarepl.console.SimpleConsole;
import javarepl.console.commands.EvaluateFromHistory;
import javarepl.console.commands.ListValues;
import javarepl.console.commands.SearchHistory;
import javarepl.console.commands.ShowHistory;
import javarepl.console.rest.RestConsole;

/**
 *
 * @author LEVALLOIS
 */
public class Controller extends Policy {

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) throws Exception {
//        System.out.println(sequence(System.getProperty("java.home").split(File.separator)).
//                        reverse().
//                        drop(1).
//                        reverse());

//        sandboxApplication();
        ConsoleConfig config = consoleConfig()
                .historyFile(new File(getProperty("user.home"), ".javarepl-embedded.history"))
                .commands(
                        ListValues.class,
                        ShowHistory.class,
                        EvaluateFromHistory.class,
                        SearchHistory.class)
                .results(
                        result("date", new Date()),
                        result("num", 42))
//                .sandboxed(true)
                ;

//        new RestConsole(new SimpleConsole(config), 8001).start();
        new Repl().main("--sandboxed", "--ignoreConsole", "--port=8001");
    }

    private static void sandboxApplication() throws UnsupportedEncodingException {
        Policy.setPolicy(new Policy() {
            private final PermissionCollection permissions = new Permissions();

            {
                permissions.add(new SocketPermission("*", "accept, connect, resolve"));
                permissions.add(new RuntimePermission("accessClassInPackage.*"));
                permissions.add(new RuntimePermission("accessClassInPackage.sun.generics.scope.*"));
                permissions.add(new RuntimePermission("accessClassInPackage.sun.misc.*"));
                permissions.add(new RuntimePermission("accessClassInPackage.sun.misc"));
                permissions.add(new RuntimePermission("getProtectionDomain"));
                permissions.add(new RuntimePermission("accessDeclaredMembers"));
                permissions.add(new RuntimePermission("createClassLoader"));
                permissions.add(new RuntimePermission("closeClassLoader"));
                permissions.add(new RuntimePermission("modifyThreadGroup"));
                permissions.add(new RuntimePermission("getStackTrace"));
                permissions.add(new ManagementPermission("monitor"));
                permissions.add(new ReflectPermission("suppressAccessChecks"));
                permissions.add(new PropertyPermission("*", "read"));
                permissions.add(new FilePermission(temporaryDirectory("JavaREPL").getAbsolutePath() + "/-", "read, write, delete"));

                Sequence<String> extensions = sequence(((URLClassLoader) getSystemJavaCompiler().getClass().getClassLoader()).getURLs()).map(URL::getPath).
                        join(paths("java.ext.dirs")).
                        join(paths("java.library.path"))
                        .append(System.getProperty("user.home"))
                        ;

                for (String extension : extensions) {
                    permissions.add(new FilePermission(extension, "read"));
                    permissions.add(new FilePermission(extension + "/-", "read"));
                }
            }

            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                return permissions;
            }
        });

        System.setSecurityManager(new SecurityManager());
    }

    private static Sequence<String> paths(String propertyKey) {
        return sequence(System.getProperty(propertyKey).split(File.pathSeparator));
    }

}
