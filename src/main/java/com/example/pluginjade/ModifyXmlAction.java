package com.example.pluginjade;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.util.Arrays;

public class ModifyXmlAction extends AnAction {

    private static final String FILE_NAME = "Jade.xml";
    private static final String RESOURCES_PATH = "src/main/resources/";
    private static final String POPUP_DIALOG_TITLE = "Sélectionnez un environnement";
    private static final String XML_FILE_EXTENSION = ".xml";
    private static final String PATH_TO_CONFIG_FILES = "/JadeConfigsXML/";

    private String pluginHoverText = "No selected env, see Jade.xml";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // Affiche la boîte de dialogue avec la liste déroulante
        ComboBox<String> comboBox = new ComboBox<>(fetchConfigFilesNames());
        int optionPaneChoice = JOptionPane.showConfirmDialog(null, comboBox, POPUP_DIALOG_TITLE, JOptionPane.DEFAULT_OPTION);
        String selectedItem = (String) comboBox.getSelectedItem();

        //Exécute le process principal du plugin seulement si on a validé la popup
        if (optionPaneChoice == JOptionPane.OK_OPTION) {
            String newConfigJadeXml = null;
            try {
                newConfigJadeXml = loadJadeConfigFromSelectedItem(selectedItem);
            } catch (IOException ex) {
                Messages.showErrorDialog(project, "La configuration pour l'environnement \"" + selectedItem + "\" n'a pas été trouvée", "Configuration Introuvable");
            }


            //Récupération du fichier Jade.xml dans le projet WebAVS
            VirtualFile jadeFileXml = findFileInProject(project, FILE_NAME);

            // Appeler la méthode de modification XML avec l'option sélectionnée
            if (jadeFileXml != null && newConfigJadeXml != null) {
                modifyXml(jadeFileXml, newConfigJadeXml);
            }

            //Modifier le contenu du hover sur icon
            pluginHoverText = "Current env : " + selectedItem;
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        //Ajuste le texte hover du plugin en fonction de l'environnement sélectionné
        e.getPresentation().setText(pluginHoverText);
    }

    /**
     * Récupère tous les noms des environnements pour matcher avec les fichiers xml de configuration
     * @return Tableau de String avec le contenu de l'Enum
     */
    private String[] fetchConfigFilesNames(){
        return Arrays.stream(EnvironnementsJade.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }

    private String loadJadeConfigFromSelectedItem(String selected) throws IOException {
        // Construire le chemin vers le fichier de configuration basé sur l'option sélectionnée
        String configFilePath = PATH_TO_CONFIG_FILES + selected + XML_FILE_EXTENSION;
        // Lire le contenu du fichier de configuration
        InputStream configInputStream = getClass().getResourceAsStream(configFilePath);
        if (configInputStream == null) {
            throw new FileNotFoundException("Fichier de configuration " + configFilePath + " introuvable dans les ressources.");
        }
        byte[] configContentBytes = configInputStream.readAllBytes();

        return new String(configContentBytes, StandardCharsets.UTF_8);
    }

    @Deprecated
    private VirtualFile findFileInProject(Project project, String fileName) {
        // Récupération du fichier Jade.xml dans le projet
        VirtualFile file = project.getBaseDir().findFileByRelativePath(RESOURCES_PATH + fileName);
        if(file != null){
           return file;
        } else {
            Messages.showErrorDialog(project, "Le fichier " + FILE_NAME + " n'a pas été trouvé dans " + RESOURCES_PATH, "Fichier Non Trouvé");
            return null;
        }
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


    /**
     * Méthode qui remplace le contenu du fichier Jade.xml par le nouveau contenu
     * @param jadeFileXml
     * @param newConfigJadeXml
     */
    private void modifyXml(VirtualFile jadeFileXml, String newConfigJadeXml) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Convertit le contenu XML en tableau d'octets
                byte[] contentBytes = newConfigJadeXml.getBytes(StandardCharsets.UTF_8);

                // Remplace directement le contenu existant par le nouveau contenu XML
                jadeFileXml.setBinaryContent(contentBytes);

                // Rafraîchit le fichier pour refléter les modifications dans l'IDE
                VfsUtil.markDirtyAndRefresh(true, true, true, jadeFileXml);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
