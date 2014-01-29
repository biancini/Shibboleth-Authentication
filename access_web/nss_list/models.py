from django.db import models

# Create your models here.
class LinuxUser(models.Model):
  INACTIVE = 'I'
  ACTIVE = 'A'
  USER_STATE = (
	(INACTIVE, 'Inactive'),
	(ACTIVE, 'Active'),
  )
  login_name = models.CharField(max_length=25)
  user_id = models.IntegerField(primary_key=True)
  group_id = models.IntegerField()
  full_name = models.CharField(max_length=100)
  home_directory = models.CharField(max_length=50)
  login_shell = models.CharField(max_length=25, default="/bin/bash")
  user_state = models.CharField(max_length=1, choices=USER_STATE, default=INACTIVE)

  def __unicode__(self):
    return self.login_name 

class LinuxGroup(models.Model):
  group_name = models.CharField(max_length=25)
  group_id = models.IntegerField(primary_key=True)
  users = models.ManyToManyField('LinuxUser', through='LinuxGroupToLinuxUser')

  def get_active_users(self):
    return self.users.filter(user_state = 'A')
  
  def __unicode__(self):
    return self.group_name 

class LinuxGroupToLinuxUser(models.Model):
  user = models.ForeignKey(LinuxUser)
  group = models.ForeignKey(LinuxGroup)
  
  def __unicode__(self):
    return "%s is in group %s" % (self.user, self.group)
