package com.PluApp.pwdmgt;

/* Class for uploading / downloading files from Google Drive.
   it must be used with DriveServiceHelper */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.PluApp.pwdmgt.DriveServiceHelper.getGoogleDriveService;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class GDriveLoader extends AppCompatActivity {

    private Context mobjAppContext = null;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int REQUEST_CODE_SIGN_IN = 100;
    public String msAccountEmail = "";
    public DriveServiceHelper mDriveServiceHelper = null;
    private static final String csModuleName = "GDriveLoader";
    public static final int ciStatusReady = 0;
    public static final int ciStatusInitializing = -1;
    public static final int ciStatusUploading = 2;
    public static final int ciStatusUploadSuccess = 20;
    public static final int ciStatusUploadFailed = 21;
    public static final int ciStatusDownloading = 1;
    public static final int ciStatusDownloadSuccess= 10;
    public static final int ciStatusDownloadFailed = 11;
    public GoogleSignInAccount gAccount = null;


    public int miStatus = ciStatusInitializing; /*    -1 : not initialize / login
                                                0 : ready
                                                1: downloading
                                                2: uploading
                                                10: download success
                                                11: download failed
                                                20: upload success
                                                21: upload failed
    */
    public static String msStatusMsg = "";


    public GDriveLoader(Context vsAppContext) {
		/* It will try to login to google on class created, if no, prompt user to login / choose from existing account */
        try {
            mobjAppContext = vsAppContext;
            gAccount = GoogleSignIn.getLastSignedInAccount(mobjAppContext);
            if (gAccount == null) {
                signIn();   // If no google account sign-in on last run, display sign-in screen
            } else {
                msAccountEmail = gAccount.getEmail(); // current email linked to google drive
                mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(mobjAppContext, gAccount, "appName"));
                miStatus = ciStatusReady;
            }
        } catch (Exception ex) {
            msStatusMsg = ex.getMessage();
        }
    }
    // google drive functions, BEGIN ===========================================================================================
    // google Sign In
    private void signIn() {
         mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    // build google sign in client
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(mobjAppContext, signInOptions);
    }
    // event on Sign-In Result triggered
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    // sign in result handler, Init DriveService Helper ==================================================================
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        msAccountEmail = googleSignInAccount.getEmail();

                        mDriveServiceHelper = new DriveServiceHelper(getGoogleDriveService(mobjAppContext, googleSignInAccount, "appName"));

                        miStatus = ciStatusReady; // ready
                        Log.d(csModuleName, "handleSignInResult: " + mDriveServiceHelper);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(csModuleName, "Unable to sign in.", e);
                    }
                });
    }
    // google drive functions, END ======================================================================================

    // popup message ------------------------------------------------------------------------------------------------
    private void popupMessage(String vsTitle, String vsMessage, String vsBtnText) {
        try {
            AlertDialog.Builder objAlrtDlg = new AlertDialog.Builder(this);
            objAlrtDlg.setTitle(vsTitle);
            objAlrtDlg.setMessage(vsMessage);
            objAlrtDlg.setPositiveButton( vsBtnText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            AlertDialog dlgAlert = objAlrtDlg.create();
            dlgAlert.show();
        } catch (Exception ex) {
            ((TextView)findViewById(R.id.txtStatus)).setText(ex.getMessage());
        }
    }

	// ==============================================================================================================================
	// function to upload file to google drive
	// parameters - vsSourceDriveFolder		- path of source file
	// 				vsUploadFile			- upload file name
	//				vbRenameExistingFile	- option to rename existing file if the target file already exist 
	
    public void uploadFile(String vsSourceGDriveFolder, String vsUploadFile, final Boolean vbRenameExistingFile) {
        if (miStatus == 0) {
            miStatus = ciStatusUploading; /*uploading */
            // upload mysql db ----------------------------------------------------------------------------------------------------------------------------------------
            final String[] aOverwrittenFile = new String[]{""};
            mDriveServiceHelper.uploadFile2Folder(
                    new java.io.File(mobjAppContext.getDatabasePath(vsUploadFile).toString()),
                    "application/x-sqlite3", vsSourceGDriveFolder, vbRenameExistingFile, aOverwrittenFile)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) { 
                            /*
                            ((Button) findViewById(R.id.create_text_file)).setEnabled(true);
                            popupMessage(CommonUtil.getLocaleStringResource(R.string.prompt_title_upload_gdrive, getApplicationContext()),
                                    CommonUtil.getLocaleStringResource(R.string.prompt_msg_upload_sqldb_gdrive_success, getApplicationContext()) +
                                            (aOverwrittenFile[0].equals("") ? "" :
                                                    (vbRenameExistingFile ? String.format(CommonUtil.getLocaleStringResource(R.string.prompt_msg_upload_gdrive_overwritten, getApplicationContext()), aOverwrittenFile[0]) : "")),
                                    CommonUtil.getLocaleStringResource(R.string.prompt_btn_ok, getApplicationContext()));
                                    */

                            miStatus = ciStatusUploadSuccess;
                            msStatusMsg = "file successfully uploaded to Google drive";
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            /*
                            ((Button) findViewById(R.id.create_text_file)).setEnabled(true);
                            popupMessage(CommonUtil.getLocaleStringResource(R.string.prompt_title_upload_gdrive, getApplicationContext()),
                                    CommonUtil.getLocaleStringResource(R.string.prompt_msg_upload_sqldb_gdrive_fail, getApplicationContext()) + e.getMessage(),
                                    CommonUtil.getLocaleStringResource(R.string.prompt_btn_ok, getApplicationContext()));*/
                            miStatus = ciStatusUploadFailed;
                            msStatusMsg = "failed to uploaded to Google drive";
                        }
                    });
        }
    }

	// ==============================================================================================================================
	// function to download file from google drive
	// parameters - vsSourceGDriveFolder	- path of download file in google drive
	// 				vsSourceFileName		- target file name
	//				vsTargetFileName		- download file path and file name 
    public void downloadFileByName(final String vsSourceGDriveFolder, final String vsSourceFileName, final String vsTargetFilename) {
        if (miStatus == ciStatusReady) {
            miStatus = ciStatusDownloading; /* searching file */
            int iRtnValue = -1;
            final String[] aTargetFileID = {""};
            final ArrayList<String> aFileList = new ArrayList<String>(), aFileIdList = new ArrayList<>();
            try {
                mDriveServiceHelper.listFileFromPath(new java.io.File(mobjAppContext.getDatabasePath(CommonUtil.csDbFile).toString()),
                        vsSourceGDriveFolder, /*csXmlFile*/ CommonUtil.csDbFile, "application/x-sqlite3", aFileList, aFileIdList)
                        .addOnSuccessListener(new OnSuccessListener<Void>() { //  success get list of available files
                            @Override
                            public void onSuccess(Void aVoid) {
                                // search for file ID of the target file in google drive
                                for (int i = 0; i < aFileList.size(); i++) {
                                    if (aFileList.get(i).equals(vsSourceFileName)) {
                                        aTargetFileID[0] = aFileIdList.get(i);
                                        break;
                                    }
                                }
								// if file ID is found, download the target file
                                if (!aTargetFileID[0].equals("")) {
                                    downloadFileByTargetID(aTargetFileID[0], vsTargetFilename);
                                } else {
                                    miStatus = ciStatusDownloadFailed;
                                    msStatusMsg = "source file not found.";
                                }

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() { // can't find from target folder or failed to get list of available files
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                /*((Button) findViewById(R.id.download_file)).setEnabled(true);
                                popupMessage(CommonUtil.getLocaleStringResource(R.string.prompt_title_download_from_gdrive, getApplicationContext()),
                                        CommonUtil.getLocaleStringResource(R.string.prompt_msg_download_gdrive_fail, getApplicationContext()) + e.getMessage(),
                                        CommonUtil.getLocaleStringResource(R.string.prompt_btn_ok, getApplicationContext()));
                                ((Button) findViewById(R.id.download_file)).setEnabled(true);*/
                                msStatusMsg = "failed to list file from source path : " + vsSourceGDriveFolder;
                                miStatus = ciStatusDownloadFailed;
                            }
                        });
            } catch (Exception ex) {

            }
        }
    }

	// ===============================================================================================
	// function to download a file from Google Drive with the file ID provided
	// parameters	- vsSourceFileID		file ID of the file to be downloaded, in Google Drive, each file has a unique file ID
	//				- vsTargetFilename		file download path and file name
    public void downloadFileByTargetID(String vsSourceFileID, String vsTargetFilename) {
        if (miStatus == ciStatusReady) {
            miStatus = ciStatusDownloading; /* downloading */
            mDriveServiceHelper.getFileById(vsSourceFileID, new java.io.File(mobjAppContext.getDatabasePath(vsTargetFilename).toString()))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                        /*popupMessage(CommonUtil.getLocaleStringResource(R.string.prompt_title_download_from_gdrive, getApplicationContext()),
                                CommonUtil.getLocaleStringResource(R.string.prompt_msg_download_gdrive_success, getApplicationContext()) + "",
                                CommonUtil.getLocaleStringResource(R.string.prompt_btn_close, getApplicationContext()));*/
                            msStatusMsg = "file successfully downloaded from google drive";
                            miStatus = ciStatusDownloadSuccess;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                        /*popupMessage(CommonUtil.getLocaleStringResource(R.string.prompt_title_download_from_gdrive, getApplicationContext()),
                                CommonUtil.getLocaleStringResource(R.string.prompt_msg_download_gdrive_fail, getApplicationContext()) + e.getMessage(),
                                CommonUtil.getLocaleStringResource(R.string.prompt_btn_close, getApplicationContext()));*/
                            msStatusMsg = "failed to download file from google drive";
                            miStatus = ciStatusDownloadFailed;
                        }
                    });
        }
    }
}
