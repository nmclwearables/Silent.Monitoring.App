package com.jakehorder.activityapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper
{
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private Drive mDriveService;


    public DriveServiceHelper(Drive mDriveService)
    {
        this.mDriveService = mDriveService;
    }

    public Task<String> createFile(final String filePath, final String fileName)
    {
        return Tasks.call(mExecutor, new Callable<String>() {
            @Override
            public String call() throws Exception {

                //String folderId = "11AzERzseERjbIRLTGn463P6JzpYl1LPm";      // Phone 1
                //String folderId = "13QGtCe4vogyrLD0IJBGRpc5Jrrx2Abkq";      // Phone 2
                //String folderId = "1YpdQ-EBDh9nMr8A8CoPeuEPK8tymxaan";      // Phone 3
                //String folderId = "1t-22JB76_RaFtzRs2raUVikdASlNEi1F";      // Phone 4
                String folderId = "1F1bWKDIO73qsjlTraJ067dVvOnD-VaNe";      // Phone 5
                File fileMetaData = new File();
                fileMetaData.setName(fileName);
                fileMetaData.setParents(Collections.singletonList(folderId));
                java.io.File file = new java.io.File(filePath);
                FileContent mediaContent = new FileContent("application/csv", file);

                File myFile = null;
                try {
                    myFile = mDriveService.files().create(fileMetaData, mediaContent)
                            .setFields("id, parents")
                            .execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (myFile == null) {
                    throw new IOException("Null result when requesting file creation");
                }
                return myFile.getId();
            }
        });
    }
}
