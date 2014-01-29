from django.contrib import admin
from nss_list.models import LinuxUser, LinuxGroup, LinuxGroupToLinuxUser

class LinuxUser_Inline(admin.TabularInline):
    model = LinuxGroupToLinuxUser

class LinuxUser_Admin(admin.ModelAdmin):
    list_display = ('login_name', 'user_id', 'group_id', 'full_name', 'home_directory', 'login_shell', 'user_state')
    search_fields = ['login_name']
    list_filter = [] #'status'

class LinuxGroup_Admin(admin.ModelAdmin):
    list_display = ('group_name', 'group_id')
    search_fields = ['group_name']
    list_filter = [] #'status'
    inlines = [LinuxUser_Inline]

admin.site.register(LinuxUser, LinuxUser_Admin)
admin.site.register(LinuxGroup, LinuxGroup_Admin)
