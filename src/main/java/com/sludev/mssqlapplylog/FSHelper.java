/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package com.sludev.mssqlapplylog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * General File-system helper methods.
 * 
 * @author Kervin Pierre
 */
public class FSHelper
{
    private static final Logger LOGGER
            = LogManager.getLogger(FSHelper.class);

    /**
     * Add the proper File-System permissions to a file so that SQL Server can run a RESTORE query.
     * 
     * @param username The username that SQL Server runs as, e.g. "NETWORK SERVICE"
     * @param file The file whose permissions will be modified.
     * @throws IOException 
     */
    public static void addRestorePermissions(String username, Path file) throws IOException
    {
        AclFileAttributeView aclAttr = Files.getFileAttributeView(file, AclFileAttributeView.class);

        UserPrincipalLookupService currULS = file.getFileSystem().getUserPrincipalLookupService();
        UserPrincipal principal = currULS.lookupPrincipalByName(username);

        AclEntry.Builder builder = AclEntry.newBuilder();
        builder.setPermissions(EnumSet.of(AclEntryPermission.READ_DATA,
                AclEntryPermission.READ_ACL,
                AclEntryPermission.READ_ATTRIBUTES,
                AclEntryPermission.READ_NAMED_ATTRS,
                AclEntryPermission.EXECUTE,
                AclEntryPermission.SYNCHRONIZE));

        builder.setPrincipal(principal);
        builder.setType(AclEntryType.ALLOW);
        aclAttr.setAcl(Collections.singletonList(builder.build()));
    }

    /**
     * List all the Backup Log Files in a directory.  
     * 
     * Optionally filter these backup files by date either using the file name
     * or Last Modified date.  Also can use an exclusion list for skipping files
     * in the returned list.
     * 
     * @param dir The backup directory containing the backup files
     * @param filterCutoff Only return files after this date
     * @param useLogFileLastMode If true, use the file's last modified date for filtering and sorting
     * @param logBackupPatternStr Regex for selecting log backup files
     * @param logBackupDatePatternStr DateTimeFormatter pattern for parsing the date from the file name
     * @param exclusionList If a file is found on this list, then it's ignored
     * @return A List of Paths that meet all query criteria
     * @throws IOException 
     */
    public static List<Path> listLogFiles( final Path dir, 
                                       final Instant filterCutoff,
                                       final boolean useLogFileLastMode, 
                                       final String logBackupPatternStr,
                                       final String logBackupDatePatternStr,
                                       final List<Path> exclusionList) throws IOException
    {
        
        Pattern filterPattern = Pattern.compile(logBackupPatternStr);

        List<Path> files = Files.list(dir).filter(i -> 
        {
            // Does the filter regex match?
            if( filterPattern.matcher(i.getFileName().toString())
                    .matches() == false )
            {
                return false;
            }
            
            // Is it on the Exclusion list?
            if( exclusionList != null 
                    && exclusionList.size() > 0
                    && exclusionList.contains(i) )
            {
                return false;
            }

            try
            {
                Instant fi;

                if (useLogFileLastMode)
                {
                    fi = Files.getLastModifiedTime(i).toInstant();
                }
                else
                {
                    fi = FSHelper.getTimestampFromFilename(logBackupPatternStr,
                            logBackupDatePatternStr, 1, i);
                }

                if (fi.isBefore(filterCutoff))
                {
                    return false;
                }
            }
            catch (IOException | RuntimeException ex)
            {
                LOGGER.warn(String.format("Error filtering '%s'", i), ex);
            }

            return true;
        })
        .sorted((a, b)
                -> 
                {
                    int modRes = 0;

                    try
                    {
                        Instant fa;
                        Instant fb;

                        if (useLogFileLastMode)
                        {
                            fa = Files.getLastModifiedTime(a).toInstant();
                            fb = Files.getLastModifiedTime(b).toInstant();
                        }
                        else
                        {
                            fa = FSHelper.getTimestampFromFilename(logBackupPatternStr,
                                    logBackupDatePatternStr, 1, a);
                            fb = FSHelper.getTimestampFromFilename(logBackupPatternStr,
                                    logBackupDatePatternStr, 1, b);
                        }

                        if (fa.isBefore(fb))
                        {
                            modRes = -1;
                        }
                        else
                        {
                            if (fb.isBefore(fa))
                            {
                                modRes = 1;
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        LOGGER.warn(
                                String.format("Error filtering '%s' and '%s'",
                                        a, b), ex);
                    }

                    return modRes;
        })
        .collect(Collectors.toList());
        
        return files;
    }
    
    /**
     * Parse a timestamp from a file name.
     * 
     * Useful for determining which Log Backup Files to process in some cases.
     * 
     * @param filePatternStr The regex that selects the files to be processed.
     * @param datePatternStr The DateTimeFormatter pattern that parses the timestamp from the file's name.
     * @param datePatternPos The group position in the file regex that contains the timestamp
     * @param path
     * @return 
     */
    public static Instant getTimestampFromFilename(final String filePatternStr,
                                                    final String datePatternStr,
                                                    final int datePatternPos,
                                                    final Path path)
    {
        Instant res = null;
        Pattern filePattern = Pattern.compile(filePatternStr);
        Matcher fbMatcher = filePattern.matcher(path.getFileName().toString());
        
        if (fbMatcher.matches())
        {
            if( fbMatcher.groupCount() < datePatternPos )
            {
                LOGGER.error(String.format("Invalid File Regex Pattern.  "
                        + "There should be at least %d date group in '%s'", 
                        datePatternPos, filePatternStr));

                return null;
            }
            
            String resStr = fbMatcher.group(datePatternPos);

            try
            {
                TemporalAccessor ta = DateTimeFormatter.ofPattern(datePatternStr)
                        .withZone(ZoneId.of("UTC"))
                        .parse(resStr);
                res = Instant.from(ta);
            }
            catch (Exception ex)
            {
                LOGGER.error(String.format("Error parsing 'Later Than' time"
                        + " from file '%s'", resStr), ex);

                return null;
            }
        }
        else
        {
            LOGGER.error(String.format("Given a Full Backup Date Pattern "
                    + "that does not match the full backup file. '%s'", filePatternStr));

            return null;
        }
        
        return res;
    }
}
