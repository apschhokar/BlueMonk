from django.conf.urls import url, patterns

urlpatterns = patterns('BlueMountain.views',
                       url(r'^uploadOperationsLog/$', 'uploadOperationsLog'),
                       url(r'^uploadMultiDbTable/$', 'uploadMultiDbTable'),
                       url(r'^uploadFileChunk/$', 'uploadFileChunk'),
                       )

