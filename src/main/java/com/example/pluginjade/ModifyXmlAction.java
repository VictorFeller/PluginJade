package com.example.pluginjade;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ModifyXmlAction extends AnAction {

    private static final String FILE_NAME = "Jade.xml";
    private static final String RESOURCES_PATH = "src/main/resources/";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Affiche la boîte de dialogue avec la liste déroulante
        //TODO VFE construire le tableau de string avec le nom des fichiers xml dans les ressources
        ComboBox<String> comboBox = new ComboBox<>(new String[]{"DEV1", "DEV2", "DEV3"});
        JOptionPane.showMessageDialog(null, comboBox, "Select Modification", JOptionPane.QUESTION_MESSAGE);
        String selected = (String) comboBox.getSelectedItem();

        String configXml = null;
        try {
            configXml = loadConfigFromSelectedItem(selected);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }


        //Récupération du file Jade.xml
        Project project = e.getProject();
        //TODO VFE implémenter exception
        VirtualFile file = findFileInProject(project, FILE_NAME);

        // Appeler la méthode de modification XML avec l'option sélectionnée
        modifyXml(file, configXml);
    }

    private String loadConfigFromSelectedItem(String selected) throws IOException {
        // Construire le chemin vers le fichier de configuration basé sur l'option sélectionnée
        String configFilePath = "/JadeConfigsXML/" + selected + ".xml";
        // Lire le contenu du fichier de configuration
        InputStream configInputStream = getClass().getResourceAsStream(configFilePath);
        if (configInputStream == null) {
            throw new FileNotFoundException("Fichier de configuration " + configFilePath + " introuvable dans les ressources.");
        }
        byte[] configContentBytes = configInputStream.readAllBytes();

        return new String(configContentBytes, StandardCharsets.UTF_8);
    }

    private VirtualFile findFileInProject(Project project, String fileName) {
        // Implémentation pour trouver le fichier dans le projet
        // Cette fonction est un exemple et doit être adaptée à vos besoins
        VirtualFile root = project.getBaseDir();
        return root.findFileByRelativePath(RESOURCES_PATH + fileName);
    }

    private void modifyXmlToAppendContent(VirtualFile file, String option) {
        try {

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file.getInputStream());

            // Normaliser le document XML
            doc.getDocumentElement().normalize();

            // Créer un nouvel élément <option>
            Element newOption = doc.createElement("option");
            newOption.appendChild(doc.createTextNode(option));

            // Ajouter cet élément à la racine ou à un élément spécifique du document XML
            // Pour cet exemple, nous l'ajoutons à l'élément racine
            doc.getDocumentElement().appendChild(newOption);

            // Écrire les modifications dans le fichier XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // Pour un formatage joli
            DOMSource source = new DOMSource(doc);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);

            // Écrire les modifications retour dans le fichier
            writeToFile(file, outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(VirtualFile file, byte[] content) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                file.setBinaryContent(content);
                // Rafraîchir le fichier pour refléter les modifications dans l'IDE
                VfsUtil.markDirtyAndRefresh(true, true, true, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void modifyXml(VirtualFile file, String newContentXml) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Convertit le contenu XML en tableau d'octets
                byte[] contentBytes = newContentXml.getBytes(StandardCharsets.UTF_8);

                // Remplace directement le contenu existant par le nouveau contenu XML
                file.setBinaryContent(contentBytes);

                // Rafraîchit le fichier pour refléter les modifications dans l'IDE
                VfsUtil.markDirtyAndRefresh(true, true, true, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
