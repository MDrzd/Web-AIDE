package org.xedox.filetree.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.xedox.filetree.utils.Node;
import org.xedox.filetree.R;

public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.VH> {

    private List<Node> nodes = new ArrayList<>();
    private Node root;
    private Context context;
    private int fileItemLayout = R.layout.file_item;
    private int indent;
    private Map<String, Integer> fileIconMappings = new HashMap<>();
    private OnFileClickListener onFileClickListener;
    private OnFileLongClickListener onFileLongClickListener;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

public List<Node> getNodes() {
    return nodes;
}

public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
    notifyDataSetChanged();
}

public Node getRoot() {
    return root;
}

public void setRoot(Node root) {
    this.root = root;
    if (root != null) {
        root.isOpen = false;
        root.level = 0;
        nodes.clear();
        nodes.add(root);
        notifyDataSetChanged();
    }
}

public Context getContext() {
    return context;
}

public void setContext(Context context) {
    this.context = context;
}

public int getFileItemLayout() {
    return fileItemLayout;
}

public void setFileItemLayout(int fileItemLayout) {
    this.fileItemLayout = fileItemLayout;
    notifyDataSetChanged();
}

public int getIndent() {
    return indent;
}

public void setIndent(int indent) {
    this.indent = indent;
    notifyDataSetChanged();
}

public Map<String, Integer> getFileIconMappings() {
    return fileIconMappings;
}

public void setFileIconMappings(Map<String, Integer> fileIconMappings) {
    this.fileIconMappings = fileIconMappings;
}

public OnFileClickListener getOnFileClickListener() {
    return onFileClickListener;
}

public void setOnFileClickListener(OnFileClickListener onFileClickListener) {
    this.onFileClickListener = onFileClickListener;
}

public OnFileLongClickListener getOnFileLongClickListener() {
    return onFileLongClickListener;
}

public void setOnFileLongClickListener(OnFileLongClickListener onFileLongClickListener) {
    this.onFileLongClickListener = onFileLongClickListener;
}

public ExecutorService getExecutor() {
    return executor;
}

public void setExecutor(ExecutorService executor) {
    this.executor = executor;
}

public Handler getHandler() {
    return handler;
}

public void setHandler(Handler handler) {
    this.handler = handler;
}

public FileTreeAdapter(Context context) {
    this.context = context;
    this.indent = (int) (40 * context.getResources().getDisplayMetrics().density);
    initDefaultIcons();
}

private void initDefaultIcons() {
    setIcon(".png", R.drawable.file_png);
    setIcon(".jpg", R.drawable.file_jpg);
    setIcon(".jpeg", R.drawable.file_jpg);
    setIcon(".js", R.drawable.javascript);
    setIcon(".css", R.drawable.css);
    setIcon(".html", R.drawable.html);
    setIcon(".htm", R.drawable.html);
    setIcon(".zip", R.drawable.zip);
    setIcon(".gz", R.drawable.zip);
    setIcon(".7z", R.drawable.zip);
    setIcon(".rar", R.drawable.zip);
    setIcon(".apk", R.drawable.apk_install);
    setIcon(".aab", R.drawable.apk_install);
    setIcon("folder", R.drawable.folder);
    setIcon("folder_open", R.drawable.folder_open);
    setIcon("folder_hidden", R.drawable.folder_hidden);
    setIcon("file_hidden", R.drawable.file_hidden);
    setIcon("file", R.drawable.file);
}

public void setIcon(String pattern, int drawableResId) {
    fileIconMappings.put(pattern, drawableResId);
}

public void removeIcon(String pattern) {
    fileIconMappings.remove(pattern);
}

public void clearIcons() {
    fileIconMappings.clear();
}

@Override
public VH onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(context).inflate(fileItemLayout, parent, false);
    return new VH(view);
}

@Override
public void onBindViewHolder(VH holder, int position) {
    Node node = nodes.get(position);

    ViewGroup.MarginLayoutParams params =
            (ViewGroup.MarginLayoutParams) holder.parent.getLayoutParams();
    params.leftMargin = indent * node.level;
    holder.parent.setLayoutParams(params);
    holder.name.setText(node.name);

    if (node.isFile) {
        setupFileIcon(holder, node);

        File file = new File(node.fullPath);
        long sizeInBytes = file.length();
        holder.size.setVisibility(View.VISIBLE);
        holder.size.setText(formatSize(sizeInBytes));

    } else {
        setupFolderIcon(holder, node);
        holder.size.setVisibility(View.GONE); // Folder tidak tampilkan size
    }

    holder.parent.setOnClickListener(v -> {
        if (node.isFile) {
            if (onFileClickListener != null) {
                onFileClickListener.onClick(node, new File(node.fullPath), v);
            }
        } else {
            toggleFolder(node);
        }
    });

    holder.parent.setOnLongClickListener(v -> {
        if (onFileLongClickListener != null) {
            onFileLongClickListener.onClick(node, new File(node.fullPath), v);
            return true;
        }
        return false;
    });
}

private void setupFileIcon(VH holder, Node node) {
    if (node.name.startsWith(".")) {
        Integer icon = fileIconMappings.get("file_hidden");
        holder.icon.setImageResource(icon != null ? icon : 0);
        return;
    }

    for (Map.Entry<String, Integer> entry : fileIconMappings.entrySet()) {
        if (node.name.endsWith(entry.getKey())) {
            holder.icon.setImageResource(entry.getValue());
            return;
        }
    }

    Integer defaultIcon = fileIconMappings.get("file");
    holder.icon.setImageResource(defaultIcon != null ? defaultIcon : 0);
}

private void setupFolderIcon(VH holder, Node node) {
    if (node.name.startsWith(".")) {
        Integer icon = fileIconMappings.get("folder_hidden");
        holder.icon.setImageResource(icon != null ? icon : 0);
        return;
    }

    String key = node.isOpen ? "folder_open" : "folder";
    Integer icon = fileIconMappings.get(key);
    holder.icon.setImageResource(icon != null ? icon : 0);
}

@Override
public int getItemCount() {
    return nodes.size();
}

public void toggleFolder(Node node) {
    node.isOpen = !node.isOpen;
    node.updateChildren();
    if (node.isOpen) {
        openFolder(node);
    } else {
        closeFolder(node);
    }
}

private int count;

private void addChildren(Node node) {
    int index = nodes.indexOf(node);
    List<Node> folders = new ArrayList<>();
    List<Node> files = new ArrayList<>();

    for (Node child : node.children()) {
        if (child.isFile) {
            files.add(child);
        } else {
            folders.add(child);
        }
    }

    Collections.sort(folders, (n1, n2) -> n1.name.compareToIgnoreCase(n2.name));
    Collections.sort(files, (n1, n2) -> n1.name.compareToIgnoreCase(n2.name));

    for (Node folder : folders) {
        nodes.add(index + 1, folder);
        count++;
        if (folder.isOpen) {
            addChildren(folder);
        }
    }

    for (Node file : files) {
        nodes.add(index + 1 + folders.size(), file);
        count++;
    }
}

public void openFolder(Node node) {
    executor.execute(() -> {
        int index = nodes.indexOf(node);
        if (index == -1) return;
        node.updateChildren();
        count = 0;
        addChildren(node);
        handler.post(() -> {
            notifyItemRangeInserted(index + 1, count);
            notifyItemChanged(nodes.indexOf(node));
        });
    });
}

public void closeFolder(Node node) {
    executor.execute(() -> {
        int parentIndex = nodes.indexOf(node);
        int removeCount = 0;
        int i = parentIndex + 1;
        while (i < nodes.size() && nodes.get(i).level > node.level) {
            removeCount++;
            i++;
        }
        if (removeCount > 0) {
            List<Node> removed = new ArrayList<>(nodes.subList(parentIndex + 1, parentIndex + 1 + removeCount));
            nodes.removeAll(removed);
            final int count = removeCount;
            handler.post(() -> {
                notifyItemRangeRemoved(parentIndex + 1, count);
                notifyItemChanged(nodes.indexOf(node));
            });
        }
        node.isOpen = false;
    });
}

public void renameNode(Node node, String name) {
    node.name = name;
    notifyItemChanged(nodes.indexOf(node));
}

public void deleteNode(Node node) {
    int parentIndex = nodes.indexOf(node);
    if (parentIndex == -1) return;
    List<Node> nodesToRemove = new ArrayList<>();
    collectChildren(node, nodesToRemove);
    nodesToRemove.add(node);
    if (!nodesToRemove.isEmpty()) {
        nodes.removeAll(nodesToRemove);
        notifyItemRangeRemoved(parentIndex, nodesToRemove.size());
    }
    node.isOpen = false;
}

private void collectChildren(Node node, List<Node> nodesToRemove) {
    for (Node child : node.children()) {
        nodesToRemove.add(child);
        if (!child.isFile && child.isOpen) {
            collectChildren(child, nodesToRemove);
        }
    }
}

public void addNode(Node parent, Node toAdd) {
    if (parent == null || toAdd == null) return;
    toAdd.level = parent.level + 1;
    parent.children.add(toAdd);
    if (parent.isOpen) {
        int parentPos = nodes.indexOf(parent);
        if (parentPos == -1) return;

        int insertPos = parentPos + 1;
        boolean isFolder = !toAdd.isFile;

        if (isFolder) {
            while (insertPos < nodes.size() && nodes.get(insertPos).level > parent.level && !nodes.get(insertPos).isFile) {
                insertPos++;
            }
        } else {
            while (insertPos < nodes.size() && nodes.get(insertPos).level > parent.level) {
                insertPos++;
            }
        }

        nodes.add(insertPos, toAdd);
        notifyItemInserted(insertPos);
    }
}

public void shutdown() {
    executor.shutdown();
}

private String formatSize(long size) {
    if (size <= 0) return "0 B";
    final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
    int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
    return String.format(Locale.getDefault(), "%.1f %s",
            size / Math.pow(1024, digitGroups), units[digitGroups]);
}

static class VH extends RecyclerView.ViewHolder {
    final View parent;
    final ImageView icon;
    final TextView name;
    final TextView size; 

    public VH(View itemView) {
        super(itemView);
        parent = itemView.findViewById(R.id.parent);
        icon = itemView.findViewById(R.id.icon);
        name = itemView.findViewById(R.id.name);
        size = itemView.findViewById(R.id.size); 
    }
}
public interface OnFileClickListener {
    void onClick(Node node, File file, View v);
}

public interface OnFileLongClickListener {
    void onClick(Node node, File file, View v);
}

// <<< TUTUP class FileTreeAdapter DI SINI
    }
