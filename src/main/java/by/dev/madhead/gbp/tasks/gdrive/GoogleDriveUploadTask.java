package by.dev.madhead.gbp.tasks.gdrive;

import by.dev.madhead.gbp.util.Constants;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import java.io.File;
import java.io.IOException;

public class GoogleDriveUploadTask extends DefaultTask {
	private String clientIdVar = Constants.DEFAULT_GDRIVE_CLIENT_ID_ENV_VAR;
	private String clientId = System.getenv(clientIdVar);
	private String clientSecretVar = Constants.DEFAULT_GDRIVE_CLIENT_SECRET_ENV_VAR;
	private String clientSecret = System.getenv(clientSecretVar);
	private String accessTokenVar = Constants.DEFAULT_GDRIVE_ACCESS_TOKEN_VAR;
	private String accessToken = System.getenv(accessTokenVar);
	private String refreshTokenVar = Constants.DEFAULT_GDRIVE_REFRESH_TOKEN_VAR;
	private String refreshToken = System.getenv(refreshTokenVar);

	// TODO: add validation
	private File archive;
	private String mimeType = MediaType.ANY_TYPE.toString();

	@TaskAction
	public void run() throws IOException {
		try {
			final HttpTransport transport = new NetHttpTransport();
			final JsonFactory jsonFactory = new JacksonFactory();
			final GoogleCredential credentials = new GoogleCredential.Builder()
					.setTransport(transport)
					.setJsonFactory(jsonFactory)
					.setClientSecrets(clientId, clientSecret)
					.build()
					.setAccessToken(accessToken)
					.setRefreshToken(refreshToken);
			final Drive drive = new Drive.Builder(transport, jsonFactory, credentials)
					.setApplicationName("backups")
					.build();

			final com.google.api.services.drive.model.File descriptor = new com.google.api.services.drive.model.File();
			final FileContent content = new FileContent(mimeType, archive);

			descriptor.setMimeType(content.getType());
			descriptor.setTitle(content.getFile().getName());

			final Drive.Files.Insert insert = drive.files().insert(descriptor, content);
			final MediaHttpUploader uploader = insert.getMediaHttpUploader();

			uploader.setChunkSize(1 * 1024 * 1024 /* bytes */);
			uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
				@Override
				public void progressChanged(MediaHttpUploader u) throws IOException {
					final double progress = (double) u.getNumBytesUploaded() / content.getLength();

					System.out.printf("\r[%-50.50s] %.2f%%", Strings.repeat("#", (int) (progress * 50)), progress * 100);
					System.out.flush();
				}
			});

			insert.execute();
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
	}

	public void setClientIdVar(String clientIdVar) {
		this.clientIdVar = clientIdVar;
		this.clientId = System.getenv(clientIdVar);
	}

	public void setClientSecretVar(String clientSecretVar) {
		this.clientSecretVar = clientSecretVar;
		this.clientSecret = System.getenv(clientSecretVar);
	}

	public void setAccessTokenVar(String accessTokenVar) {
		this.accessTokenVar = accessTokenVar;
		this.accessToken = System.getenv(accessTokenVar);
	}

	public void setRefreshTokenVar(String refreshTokenVar) {
		this.refreshTokenVar = refreshTokenVar;
		this.refreshToken = System.getenv(refreshToken);
	}

	public void setArchive(File archive) {
		this.archive = archive;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
}