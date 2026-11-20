package com.vitrola.player;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CapacitorPlugin(name = "FolderPicker")
public class FolderPickerPlugin extends Plugin {
    private static final int MAX_AUDIO_FILES = 5000;
    private static final int MAX_DEPTH = 64;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    @PluginMethod
    public void pickFolder(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(call, intent, "folderPickerResult");
    }

    @ActivityCallback
    public void folderPickerResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        Intent data = result.getData();
        if (result.getResultCode() != Activity.RESULT_OK || data == null || data.getData() == null) {
            JSObject cancelled = new JSObject();
            cancelled.put("cancelled", true);
            call.resolve(cancelled);
            return;
        }

        Uri treeUri = data.getData();
        IO.execute(() -> {
            try {
                ScanResult scan = scanTree(treeUri);
                JSObject response = new JSObject();
                response.put("cancelled", false);
                response.put("files", scan.files);
                response.put("truncated", scan.truncated);
                response.put("folderName", scan.rootName);
                getActivity().runOnUiThread(() -> call.resolve(response));
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> call.reject("Não foi possível ler a pasta selecionada.", e));
            }
        });
    }

    private ScanResult scanTree(Uri treeUri) throws Exception {
        String rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId);
        String rootName = queryDisplayName(rootDocumentUri);
        if (rootName == null || rootName.trim().isEmpty()) rootName = "Pasta";

        ScanResult result = new ScanResult(rootName);
        Set<String> visited = new HashSet<>();
        walk(treeUri, rootDocumentId, "", rootName, result, visited, 0);
        return result;
    }

    private void walk(
        Uri treeUri,
        String parentDocumentId,
        String relativeDirectory,
        String rootName,
        ScanResult result,
        Set<String> visited,
        int depth
    ) {
        if (depth > MAX_DEPTH || result.truncated || !visited.add(parentDocumentId)) return;

        ContentResolver resolver = getContext().getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
        String[] projection = new String[] {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        };

        try (Cursor cursor = resolver.query(childrenUri, projection, null, null, null)) {
            if (cursor == null) return;
            int idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);

            while (cursor.moveToNext() && !result.truncated) {
                String documentId = cursor.getString(idColumn);
                String name = cursor.getString(nameColumn);
                String mimeType = cursor.getString(mimeColumn);
                if (documentId == null || name == null) continue;

                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    String nextDirectory = relativeDirectory.isEmpty() ? name : relativeDirectory + "/" + name;
                    walk(treeUri, documentId, nextDirectory, rootName, result, visited, depth + 1);
                } else if (isAudioFile(name, mimeType)) {
                    if (result.files.length() >= MAX_AUDIO_FILES) {
                        result.truncated = true;
                        break;
                    }
                    Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                    String relativePath = rootName + "/" + (relativeDirectory.isEmpty() ? name : relativeDirectory + "/" + name);
                    JSObject file = new JSObject();
                    file.put("name", name);
                    file.put("relativePath", relativePath);
                    file.put("mimeType", mimeType == null ? "" : mimeType);
                    // Capacitor's local bridge streams this SAF URI through ContentResolver.
                    file.put("path", documentUri.toString());
                    result.files.put(file);
                }
            }
        } catch (Exception ignored) {
            // Some storage providers deny access to individual directories. Other branches remain usable.
        }
    }

    private String queryDisplayName(Uri documentUri) {
        String[] projection = new String[] { DocumentsContract.Document.COLUMN_DISPLAY_NAME };
        try (Cursor cursor = getContext().getContentResolver().query(documentUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isAudioFile(String name, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("audio/")) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.matches(".*\\.(mp3|m4a|mp4|aac|wav|wave|aif|aiff|ogg|oga|opus|flac|alac|wma|weba)$");
    }

    private static final class ScanResult {
        final String rootName;
        final JSArray files = new JSArray();
        boolean truncated;

        ScanResult(String rootName) {
            this.rootName = rootName;
        }
    }
}
