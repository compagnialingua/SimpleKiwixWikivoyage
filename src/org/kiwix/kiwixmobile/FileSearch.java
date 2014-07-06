/*
 * Copyright 2013 Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile;

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class FileSearch {

    // Array of zim file extensions
    public static final String[] zimFiles = {"zim", "zimaa"};

    // Scan through the file system and find all the files with .zim and .zimaa extensions
    public ArrayList<DataModel> findFiles() {
        String directory = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath()).toString();
        final List<String> fileList = new ArrayList<String>();
        FilenameFilter[] filter = new FilenameFilter[zimFiles.length];

        int i = 0;
        for (final String extension : zimFiles) {
            filter[i] = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith("." + extension);
                }
            };
            i++;
        }

        File[] foundFiles = listFilesAsArray(new File(directory), filter, -1);
        for (File f : foundFiles) {
            fileList.add(f.getAbsolutePath());
        }

        return createDataForAdapter(fileList);
    }

    public ArrayList<DataModel> sortDataModel(ArrayList<DataModel> data) {

        // Sorting the data in alphabetical order
        Collections.sort(data, new Comparator<DataModel>() {
            @Override
            public int compare(DataModel a, DataModel b) {
                return a.getTitle().compareToIgnoreCase(b.getTitle());
            }
        });

        return data;
    }

    // Iterate through the file system
    private Collection<File> listFiles(File directory, FilenameFilter[] filter, int recurse) {

        Vector<File> files = new Vector<File>();

        File[] entries = directory.listFiles();

        if (entries != null) {
            for (File entry : entries) {
                for (FilenameFilter filefilter : filter) {
                    if (filter == null || filefilter.accept(directory, entry.getName())) {
                        files.add(entry);
                    }
                }
                if ((recurse <= -1) || (recurse > 0 && entry.isDirectory())) {
                    recurse--;
                    files.addAll(listFiles(entry, filter, recurse));
                    recurse++;
                }
            }
        }
        return files;
    }

    private File[] listFilesAsArray(File directory, FilenameFilter[] filter, int recurse) {
        Collection<File> files = listFiles(directory, filter, recurse);

        File[] arr = new File[files.size()];
        return files.toArray(arr);
    }

    // Create an ArrayList with our DataModel
    private ArrayList<DataModel> createDataForAdapter(List<String> list) {

        ArrayList<DataModel> data = new ArrayList<DataModel>();
        for (String file : list) {

            data.add(new DataModel(getTitleFromFilePath(file), file));
        }

        data = sortDataModel(data);

        return data;
    }

    // Remove the file path and the extension and return a file name for the given file path
    private String getTitleFromFilePath(String path) {
        return new File(path).getName().replaceFirst("[.][^.]+$", "");
    }
}
