# mssqlapplylogs
Continously apply Microsoft SQL Server Log Backups without configuring Log Shipping on the receiving end.

## Motivation
This project aims to provide log shipping to Microsoft SQL Server instances that may not be able to directly connect to the primary database.  This may be due to network rules beyond your control, or security policies for example.

Regular Log Shipping is configured on the internal SQL Server instance, but the Log Backup files e.g. **.trn* files are transferred *out-of-band*. For instance we transfer our Log Backups via SSH and RSync on Linux servers.

![A simple MSSQLApplyLogs Network](https://raw.githubusercontent.com/kervinpierre/mssqlapplylogs/master/docs/images/mssqlapplylog_networkdiagram01.png)

## Feature Overview

MSSQLApplyLogs provides a process that 
* Restores the last full backup for your database
* Parses your full backup file name for its creation date.
  * Optionally uses the backup file's Last Modified File-system attribute for its Creation Time
* Searches your local folder for all Transaction Log Backups beyond your full backup date and Restores those.
* Optionally listens to your local backup folder for new backup files.  Processing those as they are created.
* Add permissions to the Log Backup files before SQL Server attempts to run the RESTORE query.
* Override most options in the properties file or command line interface.
* 
Example usage on the command line looks like...
```
PS C:\syncbackups\target> java -jar .\mssqlapplylog-1.0.jar --conf conf.properties --restore-full
```

Usage message from the command line...
```
usage:
java -jar mssqlapplylog-1.0.jar  [--conf <arg>] [--laterthan <arg>]
       [--monitor-backup-dir] [--restore-full] [--use-lastmod]

The MSSQLApplyLog application can be used in a variety of options and modes.
   --conf <arg>          Configuration file.
   --laterthan <arg>     'Later Than' file filter.
   --monitor-backup-dir  Monitor the backup directory for new log backups, and
                         apply them.
   --restore-full        Restore the full backup before continuing.
   --use-lastmod         Sort/filter the log backups using their File-System
                         'Last Modified' date.
```

## Development
This application was created using Java 8 in a very short amount of time.  But it is being used in production and tested in at least this particular usecase.  

A lot more testing and features are definitely possible. 

