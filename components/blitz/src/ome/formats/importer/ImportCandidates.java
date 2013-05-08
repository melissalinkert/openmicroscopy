/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.formats.importer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import loci.formats.DirectoryParser;
import loci.formats.FileInfo;
import loci.formats.IFormatReader;
import loci.formats.MissingLibraryException;
import loci.formats.UnknownFormatException;
import loci.formats.UnsupportedCompressionException;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import ome.formats.ImageNameMetadataStore;
import ome.formats.importer.util.ErrorHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class which given any {@link File} object will determine the correct
 * number and members of a given import. This facility permits iterating over a
 * directory.
 *
 * This class is NOT thread-safe.
 *
 * @since Beta4.1
 */
public class ImportCandidates extends DirectoryParser
{

    /**
     * Event raised during a pass through the directory structure given to
     * {@link ImportCandidates}. A {@link Scanning} event will not necessarily
     * be raised for every file or directory, but the values will be valid for
     * each event.
     *
     * If {@link #totalFiles} is less than 0, then the directory is currently be
     * scanned and the count is unknown. Once {@link #totalFiles} is positive,
     * it will remain constant.
     *
     * If {@link #cancel()} is called, then directory searching will cease. The
     * {@link ImportCandidates} instance will be left with <em>no</em>
     * {@link ImportContainer}s.
     */
    public static class SCANNING extends ImportEvent
    {
        public final File file;
        public final int depth;
        public final int numFiles;
        public final int totalFiles;
        private boolean cancel = false;

        public SCANNING(File file, int depth, int numFiles, int totalFiles)
        {
            this.file = file;
            this.depth = depth;
            this.numFiles = numFiles;
            this.totalFiles = totalFiles;
        }

        /**
         * Can be called to cancel the current action.
         */
        public void cancel()
        {
            this.cancel = true;
        }

        public String toLog()
        {
            int l = file.toString().length() - 16;
            if (l < 0)
            {
                l = 0;
            }
            String f = file.toString().substring(l);
            return super.toLog() + String.format(": Depth:%s Num: %4s Tot: %4s File: %s",
                    depth, numFiles, (totalFiles < 0 ? "n/a" : totalFiles), f);
        }
    }

    final private static Logger log = LoggerFactory.getLogger(ImportCandidates.class);

    final public static int DEPTH = Integer.valueOf(
            System.getProperty("omero.import.depth","4"));
    final public static MetadataLevel METADATA_LEVEL =
        MetadataLevel.valueOf(System.getProperty(
                "omero.import.metadata.level","MINIMUM"));

    final private IObserver observer;
    final private OMEROWrapper reader;
    final private List<ImportContainer> containers = new ArrayList<ImportContainer>();

    /**
     * Calls {@link #ImportCandidates(int, OMEROWrapper, String[], IObserver)}
     * with {@link #DEPTH} as the first argument.
     *
     * @param reader
     *            instance used for parsing each of the paths. Not used once the
     *            constructor completes.
     * @param paths
     *            file paths which are searched. May be directories.
     * @param observer
     *            {@link IObserver} which will monitor any exceptions during
     *            {@link OMEROWrapper#setId(String)}. Otherwise no error
     *            reporting takes place.
     */
    public ImportCandidates(OMEROWrapper reader, String[] paths,
            IObserver observer)
    {
        this(DEPTH, reader, paths, observer);
    }

    /**
     * Main constructor which iterates over all the paths calling
     * {@link #walk(File, Collection)} and permitting a descent to the given
     * depth.
     *
     * @param depth
     *            number of directory levels to search down.
     * @param reader
     *            instance used for parsing each of the paths. Not used once the
     *            constructor completes.
     * @param paths
     *            file paths which are searched. May be directories.
     * @param observer
     *            {@link IObserver} which will monitor any exceptions during
     *            {@link OMEROWrapper#setId(String)}. Otherwise no error
     *            reporting takes place.
     */
    public ImportCandidates(int depth, OMEROWrapper reader, String[] paths,
            IObserver observer)
    {
        super(reader, depth, new DefaultMetadataOptions(METADATA_LEVEL));
        this.reader = reader;
        this.observer = observer;

        if (paths != null && paths.length == 2 && "".equals(paths[0])
                && "".equals(paths[1]))
        {
            // Easter egg for testing.
            // groups is not null, therefore usage() won't need to be
            // called.
            System.exit(0);
            return;
        }

        List<FileInfo> filesets = getFilesets(paths);
        ImportConfig config = reader.getConfig();
        String configImageName = config.userSpecifiedName.get();
        for (FileInfo fileset : filesets) {
          ImportContainer ic = new ImportContainer(
            new File(fileset.filename), null, null,
            fileset.reader.getCanonicalName(),
            fileset.usedFiles, fileset.isSPW);
          ic.setDoThumbnails(config.doThumbnails.get());

          if (configImageName == null) {
            ic.setUserSpecifiedName(fileset.filename);
          }
          else {
            ic.setUserSpecifiedName(configImageName);
          }
          ic.setUserSpecifiedDescription(config.userSpecifiedDescription.get());
          ic.setCustomAnnotationList(config.annotations.get());

          containers.add(ic);
        }
    }

    /**
     * Prints the "standard" representation of the groups, which is parsed by
     * other software layers. The format is: 1) any empty lines are ignored, 2)
     * any blocks of comments separate groups, 3) each group is begun by the
     * "key", 4) all other files in a group will also be imported.
     *
     * Similar logic is contained in {@link Groups#print()} but it does not
     * take the ordering of the used files into account.
     */
    public void print()
    {
        if (containers == null)
        {
            return;
        }
        for (ImportContainer container : containers)
        {
            System.out.println("#======================================");
            System.out.println(String.format(
                    "# Group: %s SPW: %s Reader: %s", container.getFile(),
                    container.getIsSPW(), container.getReader()));
            for (String file : container.getUsedFiles())
            {
                System.out.println(file);
            }
        }
    }

    /**
     * @return all containers as a array list
     */
    public List<ImportContainer> getContainers()
    {
        return new ArrayList<ImportContainer>(containers);
    }

    /**
     * @param f
     * @param d
     * @throws CANCEL
     */
    @Override
    protected void scanWithCancel(File f, int d) throws CANCEL{
        SCANNING s = new SCANNING(f, d, count, total);
        safeUpdate(s);
        if (s.cancel) {
            throw new CANCEL();
        }
    }

    @Override
    protected void safeUpdate(String path, Throwable t) {
      // TODO
    }

    /**
     * Update observers with event
     *
     * @param event
     */
    private void safeUpdate(ImportEvent event) {
        try {
            observer.update(null, event);
        } catch (Exception ex) {
            log.error(
                    String.format("Error on %s with %s", observer, event),
                    ex);
        }
    }

}
