from django.conf.urls.defaults import patterns, include, url

from views import passwd, group, register

urlpatterns = patterns('',
    url(r'^passwd', passwd),
    url(r'^group', group),
    url(r'^register', register),
)

