# gradle-backup-plugin
[![Build Status](https://travis-ci.org/madhead/gradle-backup-plugin.svg?branch=master)](https://travis-ci.org/madhead/gradle-backup-plugin)
[![Dependency Status](https://www.versioneye.com/user/projects/552801ed2ced4f5816000c53/badge.svg?style=flat)](https://www.versioneye.com/user/projects/552801ed2ced4f5816000c53)
[![Download latest version](https://api.bintray.com/packages/madhead/gradle-plugins/gradle-backup-plugin/images/download.svg) ](https://bintray.com/madhead/gradle-plugins/gradle-backup-plugin/_latestVersion)

This [Gradle](http://gradle.org/) plugin allows you to automate small non-production backups and upload them into various clouds.

Currently it can upload things to:

+ [Google Drive](https://www.google.com/drive/)

You can create [ZIP](https://gradle.org/docs/current/dsl/org.gradle.api.tasks.bundling.Zip.html) and [TAR](https://gradle.org/docs/current/dsl/org.gradle.api.tasks.bundling.Tar.html) archives using awesome Gradle's out-of-the-box features! And if you're really paranoid you can encrypt them with your favourite Java encryption library before the uploading.

## Usage

The plugin is available via [jCenter](https://bintray.com/bintray/jcenter) repository. To use it, add the following lines into your `build.gradle`:

	buildscript {
		repositories {
			jcenter()
		}

		dependencies {
			classpath 'by.dev.madhead:gradle-backup-plugin:latest.release'
		}
	}

Latest snapshots are avaiable at [OJO](https://oss.jfrog.org). To use them, add the following:

	buildscript {
		repositories {
			maven {
				url 'http://oss.jfrog.org/oss-snapshot-local'
			}
		}

		dependencies {
			classpath 'by.dev.madhead:gradle-backup-plugin:1.0.5-SNAPSHOT'
		}
	}

After that your buildscript will be enhanced with the task types from the plugin.

### Creating archive

First, you need to create a backup archive. It can be ZIP or TAR (they are very simple to create with Gradle), or custom archive (in this case you might need to write a few lines of your own code). For example, creating TAR archive:

	task createTarball(type: Tar) {
		baseName = 'calibre-' + new Date().format('yyyy-MM-dd_hh-mm')
		destinationDir = project.buildDir
		compression = Compression.GZIP
		from project.projectDir
		excludes = [
			'build/**',
			'build.gradle'
		]
	}

After the archive is ready you want to upload it into a cloud.

### Uploading to the Google Drive

You'll need to create your own project in [Google Developers Console](https://console.developers.google.com). Don't worry, it's very easy and free.

Create a project with any name and ID you like. After that, open it and go to `APIs & auth` → `APIs`. Search for `Drive API` and enable it. Then, navigate to `APIs & auth` → `Credentials` and create new OAuth 2.0 app (`Create new Client ID`). Choose `Installed application` with type `Other`. You might be asked to fill `Consent screen` before being able to create the app. That's ok, data on that screen will be seen only by you.

After the app is created, store it's `Client ID` and `Client Secret` in environment variables on your system for future access with `System.getenv()`. You might choose another way to store them and pass to the tasks.

Before talking to Google Drive API, you need to grant access token to the app you've created. The plugin contains a class named `ObtainGoogleDriveTokensTask` which will help you:

	task obtainGoogleDriveTokens(type: by.dev.madhead.gbp.tasks.gdrive.ObtainGoogleDriveTokensTask) {
		clientId = System.getenv('CALIBRE_BACKUP_GDRIVE_CLIENT_ID')
		clientSecret = System.getenv('CALIBRE_BACKUP_GDRIVE_CLIENT_SECRET')
	}

Run `gradle obtainGoogleDriveTokens`, follow the instructions and you'll get `Access Token` and `Refresh Token` which are used to communicate the Google Drive API. Store them in environment variables too.

Now all you need to do is to configure `upload` task:

	task backup(type: by.dev.madhead.gbp.tasks.gdrive.GoogleDriveUploadTask) {
		clientId = System.getenv('CALIBRE_BACKUP_GDRIVE_CLIENT_ID')
		clientSecret = System.getenv('CALIBRE_BACKUP_GDRIVE_CLIENT_SECRET')
		accessToken = System.getenv('CALIBRE_BACKUP_GDRIVE_ACCESS_TOKEN')
		refreshToken = System.getenv('CALIBRE_BACKUP_GDRIVE_REFRESH_TOKEN')

		dependsOn createTarball                 // 1
		mimeType = 'application/x-gtar'         // 2
		archive = createTarball.archivePath     // 3
		path = ['Backups', 'Calibre']           // 4
		listenForUpload = true                  // 5
	}

1. Tolds Gradle to execute `createTarball` before uploading the results.
2. Helps Google Drive to recognize what is being uploaded. You'll be able to work with archive in the cloud without downloading if you have apps for that MIME type installed.
3. Specifies the file to upload.
4. Specifies destination path inside Google Drive starting from the root to put the archive.
5. Enables drawing text-based progress bar tracking the upload process.

That's all. Run `gradle backup` and you'll get your backup in a cloud:

![image](https://cloud.githubusercontent.com/assets/577360/7076097/a7a127d8-df0f-11e4-831b-ae9eed8bc4ae.png)
