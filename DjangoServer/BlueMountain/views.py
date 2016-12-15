import traceback

from django.shortcuts import HttpResponse
from django.views.decorators.csrf import csrf_exempt

from DjangoServer import settings

# Command Line Argument to run the Server
# For running on localhost: "python manage.py runserver"
# For running on Public IP: "python manage.py runserver 0.0.0.0:8000"

FILE_CHUNK_DB_FILE = settings.BASE_DIR + '/bm_droids_db_cloud.db'
MULTI_DB_TABLE_FILE = settings.BASE_DIR
OP_LOG_FILE = settings.BASE_DIR + '/log_file.txt'


# Server implementation of File Chunking Approach
# Author Ramanpreet Singh Khinda
@csrf_exempt
def uploadFileChunk(request):
    if request.method == "POST":
        try:
            chunk_byte_data = request.FILES["chunkByteData"]
            offset = request.POST.get('offset', None)

            try:
                file_chunk_db_file = open(FILE_CHUNK_DB_FILE, 'r+b')
            except IOError:
                file_chunk_db_file = open(FILE_CHUNK_DB_FILE, 'wb')

            file_chunk_db_file.seek(int(offset))
            file_chunk_db_file.write(chunk_byte_data.read())
            file_chunk_db_file.close()

            return HttpResponse(True)

        except Exception as e:
            traceback.print_stack()
            return HttpResponse(False)

    else:
        return HttpResponse(False)


# Server implementation of Multi DB Approach
# Author Ajay Pratap Singh
@csrf_exempt
def uploadMultiDbTable(request):
    if request.method == "POST":
        try:
            db_table_file = request.FILES["dbTableFile"]
            db_table_file_name = request.FILES["dbTableFile"].name
            destination = open(MULTI_DB_TABLE_FILE + "/" + db_table_file_name, 'wb')

            for chunk in db_table_file.chunks():
                destination.write(chunk)

            destination.close()
            return HttpResponse(True)

        except Exception as e:
            traceback.print_stack()
            return HttpResponse(False)

    else:
        return HttpResponse(False)


# Server implementation of Operations Log Approach
# Author Anirudh
# Todo this is just sample implementation: Actual implementation is in Node JS Server
@csrf_exempt
def uploadOperationsLog(request):
    if request.method == "POST":
        try:
            log_file = request.FILES["logFile"]
            destination = open(OP_LOG_FILE, 'wb')

            for chunk in log_file.chunks():
                destination.write(chunk)

            destination.close()
            return HttpResponse(True)

        except Exception as e:
            traceback.print_stack()
            return HttpResponse(False)

    else:
        return HttpResponse(False)
