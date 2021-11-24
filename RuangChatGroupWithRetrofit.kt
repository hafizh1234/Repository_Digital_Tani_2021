package id.ac.ipb.mobile.digitani

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import id.ac.ipb.mobile.digitani.adapter.AdapterChatGroup
import id.ac.ipb.mobile.digitani.api.ApiClient
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.ActivityRuangChatGroupBinding
import id.ac.ipb.mobile.digitani.helper.GetPathFromUriHelper
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.*
import id.ac.ipb.mobile.digitani.response.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

//mengambil seluruh chat, mengirim foto, attachment, dan menampilkan chat yang belom dihapus
class RuangChatGroup : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiInterface
    private lateinit var arrayListOfIsiRoomChat: ArrayList<ChatRoomMemberData>

    private lateinit var isiPesan: String
    private lateinit var arrayListOfApprovedMemberInThisRoom: ArrayList<ChatRoomMemberData>

    private lateinit var currentPhotoPath: String
    private var mediaUri: Uri? = null
    private var isApproved: Boolean = false
    private var cameraPermitted: Boolean = false
    private var externalStorage: Boolean = false
    private lateinit var memberCurrent: ChatRoomMemberData
    private var mimeType: String? = null
    private var isOpeningAttachment: Boolean = false

    private lateinit var messagesInARoom: ArrayList<MessageIsi>
    private lateinit var adapter: AdapterChatGroup
    private lateinit var etIsiPesan: EditText
    private lateinit var arrayListOfUserInRoom: ArrayList<User>
    lateinit var messagesInARoomSimple: ArrayList<OneMessageForOneMember>
    private lateinit var binding: ActivityRuangChatGroupBinding

    private var ruangChat: RuangChat? = null
    private var requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
            it.entries.forEach {
                if (it.key == "android.permission.CAMERA") {
                    if (it.value == false) {
                        Toast.makeText(
                            this,
                            "Kamera harus diizinkan untuk menggunakan kamera",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        cameraPermitted = true
                    }
                } else if (it.key == "android.permission.WRITE_EXTERNAL_STORAGE") {
                    if (it.value == false) {
                        Toast.makeText(
                            this,
                            "Penyimpanan harus diizinkan untuk menyimpan hasil kamera",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        externalStorage = true
                    }
                }

            }

        }

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uri ->
                //ambil foti dari galeri dengan mengambil foto berekstensi png dan jpeg/jpg
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    currentPhotoPath = GetPathFromUriHelper.getPath(this, uri).toString()

                    val string = currentPhotoPath.split(".")
                    for (isi in string) {
                        if (isi == string[string.size - 1]) {
                            if (isi == "png") {
                                mimeType = "image/png"
                            } else if (isi == "jpg" || isi == "jpeg") {
                                mimeType = "image/jpeg"
                            }
                        }
                    }

                } else {
                    Toast.makeText(
                        this,
                        "Maaf versi android yang dibutuhkan minimal adalah versi Kitkat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    private val takePictureContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                //ambil foto dengan kamera, namakan file tersebut dengan nama "ddMMyyyy_HHmmss"
                // dan masukkan ke dalam storage penyimpanan melalui ContentResolver
            }
        }

    companion object {
        const val EXTRA_RUANG = "Extra Ruang"
        const val Is_Approved = "Is_Approved"
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //inisialisasi variabel
        ruangChat = intent.getParcelableExtra<RuangChat>(EXTRA_RUANG)
        isApproved = intent.getBooleanExtra(Is_Approved, false)

        if (ruangChat != null) {
            binding.sendButtonChatLog.visibility = View.VISIBLE
            binding.tvGroupName.text = ruangChat!!.name
            binding.tvGroupDescription.text = ruangChat!!.description
        }
        binding.btnAttachment.setOnClickListener {
            if (!isOpeningAttachment) {
                binding.cvAttachment.visibility = View.VISIBLE
                isOpeningAttachment = true
            } else {
                binding.cvAttachment.visibility = View.GONE
                isOpeningAttachment = false
            }
        }
        binding.cancelAddGambarToGroup.setOnClickListener {
            if (isOpeningAttachment) {
                binding.cvAttachment.visibility = View.GONE
                isOpeningAttachment = false
            }
        }
        binding.btnPickGalleryImageGroup.setOnClickListener {
            pickImages.launch("image/*")
        }

        adapter.setOnItemClickCallback(object : AdapterChatGroup.OnItemClickCallback {
            override fun onItemClicked(message: OneMessageForOneMember) {
                unduhGambar(message.image, message.image_name)
            }

            override fun onItemLongClicked(message: OneMessageForOneMember): Boolean {
                showDialogFrag("hapusPesan", message)
                return true
            }
        })
        //kalo belum approved, batasi penerimaan all message dan kirim message
        binding.sendButtonChatLog.setOnClickListener {
            //liat apakah isi text messagesnya null sama liat kirim pake gambar nggak
            isiPesan = etIsiPesan.text.toString()
            if (isiPesan.isEmpty()) {
                binding.etTextDraft.error =
                    "Silahkan masukkan pesan terlebih dahulu."
            } else {
                if (isApproved) {
                    if (mediaUri != null) {
                        uploadFile(isiPesan)
                    } else {
                        kirimPesan(isiPesan)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Anda Belum Diterima masuk ke dalam grup ini. Sehingga anda tidak dapat mengirim atau melihat pesan apapun.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        binding.btnCameraAttachmentGroup.setOnClickListener {
            if (cameraPermitted && externalStorage) {
                takePic()
            }
        }
        binding.cancelImage.setOnClickListener {
            binding.cvImageTerpilih.visibility = View.GONE
            mediaUri = null
            mimeType = null
        }

    }

    private fun updateMessage(message: OneMessageForOneMember) {
        //menghapus pesan dengan cara mengupdate atribut pesan, yaitu deleted_by_str sehingga pesan tetap ada dalam database dan dapat dilihat oleh anggota grup lain.
        //var harusDiDelete adalah variabel yang menyatakan bahwa semua anggota grup sudah menghapus salah satu pesan yang sama di dalam grup.
        //jika false maka masih ada anggota dalam grup ini yang belum menghapus pesan tersbut. Namun, jika true maka semua anggoat grup sudah menghapus satu pesan yang sama.
        if (!harusDiDelete) {
            //keteranganPalingAkhir adalah nilai untuk mengisi deleted_by_str yang menyimpan idUser yang seudah menghapus pesan.

            apiService = ApiClientChatNew.chatRoomApiClient
            apiService.updateMessage(
                "Bearer $token",
                message.id,
                message.chat_member_id.toString(),
                message.content,
                keteranganPalingAkhir,
                message.type
            )
                .enqueue(object : Callback<MessageRespon> {
                    override fun onResponse(
                        call: Call<MessageRespon>,
                        response: Response<MessageRespon>
                    ) {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Hapus Pesan Berhasil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    override fun onFailure(call: Call<MessageRespon>, t: Throwable) {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Gagal menghapus Pesan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }
    //menghapus pesan secara hard delete dikarenakan pesan sudah tidak dibutuhkan lagi oleh seluruh anggota grup dan semua nggota grup menghapus pesan ini.
    private fun deleteMessage(messageId: Int, semuaPesanDiruanganSudahDihapus: Boolean) {

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteMessage("Bearer $token", messageId)
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    var result = response.body()?.result
                    if (result == 1) {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Anda berhasil menghapus pesan",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Gagal menghapus Pesan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    Toast.makeText(this@RuangChatGroup, "Gagal mengambil data", Toast.LENGTH_LONG)
                        .show()
                }
            })
    }

    private fun showDialogFrag(keterangan: String, pesan: OneMessageForOneMember?) {
        var dialogMessage: String = ""
        var dialogTitle: String = ""
        if (keterangan == "hapusPesan") {
            dialogMessage = "Apakah anda ingin menghapus pesan ini?"
            dialogTitle = "Hapus Pesan"
        }
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                if (keterangan == "hapusPesan") {
                    updateMessage(pesan!!)
                }
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

    }

    @Throws(IOException::class)
    fun takePic() {
        try {
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            val folder = File(filesDir, "DigitaniIPB")
            val file = File(folder, "JPEG_${timeStamp}")

            if (!folder.exists()) {
                folder.mkdirs()
            }
            if (file.createNewFile() || file.exists()) {
                mediaUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                takePictureContract.launch(mediaUri)
            } else {
                null
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun unduhGambar(image: String, imageName: String) {
        apiService = ApiClient.getGambarInMessage
        apiService.getGambarInPesan("$image")
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        var hasilImage = response.body()
                        if (hasilImage != null) {
                            writtenToDisk(hasilImage, imageName)
                        }
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatGroup,
                        "Gagal mendownload gambar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
    private fun kirimPesan(isiPesan: String) {
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.postMessages(
            "Bearer $token",
            memberOfGroup!!.id.toString(),
            isiPesan,
            "CHAT_MESSAGE:ONLY_TEXT"
        )
            .enqueue(object : Callback<MessageRespon> {
                override fun onResponse(
                    call: Call<MessageRespon>,
                    response: Response<MessageRespon>
                ) {
                    if (response.isSuccessful) {
                        binding.etTextDraft.text = null
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Pesan berhasil dikirim",
                            Toast.LENGTH_SHORT
                        ).show()
                        getAllMembersInGroup()
                    } else {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Anda belum bisa mengirim chat",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MessageRespon>, t: Throwable) {

                }

            })
    }

    private fun writtenToDisk(body: ResponseBody, imageName: String): Boolean {
        //implementasi penulisan file di dalam storage
        return true
    }
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun uploadFile(isiPesan: String?) {
        var file: File? = null
        val detailUser = sessionManager!!.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var memberOfGroup: ChatRoomMemberData? = null
        var idUserSekarang: Int = detailUser[SessionManager.KEY_ID_USER] as Int

        for (isi in arrayListOfIsiRoomChat) {
            if (isi.user_id == idUserSekarang) {
                memberOfGroup = isi
            }
        }
        apiService = ApiClientChatNew.chatRoomApiClient

        if (mediaUri != null) {
            currentPhotoPath = GetPathFromUriHelper.getPath(this, mediaUri!!).toString()
            file = File(currentPhotoPath)

        }
        //mengambil isi file gambar dengan requestbody
        val requestFile = file!!.asRequestBody(
            mediaUri?.let { contentResolver.getType(it) }!!
                .toMediaTypeOrNull()
        )
        // MultipartBody.Part mengirimkan nama, dan
        val body: MultipartBody.Part =
            MultipartBody.Part.createFormData("image", file.name, requestFile)
        var idUser = idUserSekarang.toString().toRequestBody("text/plain".toMediaType())
        var memberId = memberOfGroup!!.id.toString().toRequestBody("text/plain".toMediaType())
        var content = isiPesan?.toRequestBody("text/plain".toMediaType())
        var type = "CHAT_MESSAGE:WITH_IMAGE".toRequestBody("text/plain".toMediaType())

        if (content != null) {
            apiService.postMessageWithImage("Bearer $token", body, memberId, content, type)
                .enqueue(object : Callback<MessageRespon> {
                    override fun onResponse(
                        call: Call<MessageRespon>,
                        response: Response<MessageRespon>
                    ) {
                        if (response.isSuccessful) {
                            //var message: Message = response.body()!!.chat_message
                            Toast.makeText(
                                this@RuangChatGroup,
                                "Pesan dengan gambar berhasil dikirim",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.etTextDraft.text = null
                            binding.cvImageTerpilih.visibility = View.GONE
                            mediaUri = null
                            mimeType = null

                            getAllMembersInGroup()
                        } else {
                            Toast.makeText(
                                this@RuangChatGroup,
                                "Gambar terlalu besar.Silahkan kirim gambar lain max 1MB",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                    override fun onFailure(call: Call<MessageRespon>, t: Throwable) {
                        Toast.makeText(
                            this@RuangChatGroup,
                            "Gagal Mengirim Pesan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })
        }
    }

    //untuk mulai chat, identifikasi dulu usernya di grup chat ini. Lalu simpan chat room id masing masing
    private fun getAllMembersInGroup() {

        apiService = ApiClientChatNew.chatRoomApiClient

        apiService.getAllMembersInOneRoom("Bearer $token", ruangChat?.id.toString())
            .enqueue(object : Callback<ChatRoomMemberResponse> {
                override fun onResponse(
                    call: Call<ChatRoomMemberResponse>,
                    response: Response<ChatRoomMemberResponse>
                ) {
                    if (response.isSuccessful) {
                        var chatRoomMember = response.body()?.chat_members
                        //maksimal 500 member
                        if (chatRoomMember != null) {
                            arrayListOfIsiRoomChat.addAll(chatRoomMember)
                        }
                        for (item in arrayListOfIsiRoomChat) {
                            if (item.is_approved) {
                                arrayListOfApprovedMemberInThisRoom.add(item)
                            }
                        }

                        memberCurrent =
                            arrayListOfIsiRoomChat.firstOrNull { it.user_id == idUser }!!
                        if (!memberCurrent.is_approved) {
                            binding.progressbarRuangGrup.visibility = View.GONE
                        }
                        getAllMessagesInGroup()
                    }
                }


                override fun onFailure(call: Call<ChatRoomMemberResponse>, t: Throwable) {
                    Toast.makeText(
                        this@RuangChatGroup,
                        "Gagal mengambil data",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

    }

    private fun getAllMessagesInGroup() {
        val detailUser = sessionManager!!.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        messagesInARoomSimple.clear()
        apiService = ApiClientChatNew.chatRoomApiClient
        //ambil semua message
        //filter room yang sesuai
        apiService.getAllMessage("Bearer $token")
            .enqueue(object : Callback<GetChatMessages> {
                override fun onResponse(
                    call: Call<GetChatMessages>,
                    response: Response<GetChatMessages>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            binding.sendButtonChatLog.visibility = View.VISIBLE
                            messagesInARoom = response.body()!!.chat_messages
                            for (item in messagesInARoom) {
                                adapter.listMessages = messagesInARoomSimple
                                adapter.notifyDataSetChanged()
                                getAllUser()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<GetChatMessages>, t: Throwable) {

                }
            })
    }

    private fun getAllUser() {

        apiService = ApiClient.userApiClientGetAllUsers
        apiService.getUserAll("Bearer $token")
            .enqueue(object : Callback<Users> {
                override fun onResponse(call: Call<Users>, response: Response<Users>) {
                    if (response.isSuccessful) {
                        users = response.body()!!.users

                        if (users.isEmpty()) {
                            Toast.makeText(
                                this@RuangChatGroup,
                                "Tidak ada user",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            binding.sendButtonChatLog.visibility = View.VISIBLE
                            for (item in users) {
                                //ambil semua user yang ada dan simpan ke dalam arrayOfUser untuk menampilkan nama user pada setiap chat yang ada di grup
                            }
                            adapter.arrayListOfUserInRoom = arrayListOfUserInRoom
                            adapter.notifyDataSetChanged()
                        }

                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {

                }

            })
    }
}
