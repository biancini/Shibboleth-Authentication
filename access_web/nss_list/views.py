# Create your views here.

from django.shortcuts import render
from django.contrib.auth.decorators import login_required

from models import LinuxUser
from models import LinuxGroup

def passwd(request):
  return render(request, "passwd.html", {"users": LinuxUser.objects.all()})

def group(request):
  return render(request, "group.html", {"groups": LinuxGroup.objects.all()})

  
def find_uid(proposed):
  users = LinuxUser.objects.all()
  valid = True

  if proposed is not None:
    for user in users:
      if user.user_id == proposed:
        valid = False
  else:
    valid = False

  if valid: return proposed
  
  max_uid = 2000
  for user in users:
    if user.user_id > max_uid:
      max_uid = user.user_id

  return max_uid + 1

def find_gid(proposed):
  users = LinuxGroup.objects.all()
  valid = True

  if proposed is not None:
    for user in users:
      if user.group_id == proposed:
        valid = False
  else:
    valid = False

  if valid: return proposed
  
  max_gid = 2000
  for user in users:
    if user.group_id > max_gid:
      max_gid = user.group_id

  return max_gid + 1


@login_required
def register(request):
  users = LinuxUser.objects.all()
  groups = LinuxGroup.objects.all()
  
  cn = request.META.get('cn', request.user.username)
  userid = request.META.get('uid', cn.split(' ')[0].lower())
  uidNumber = request.META.get('uidNumber', None)
  gidNumber = request.META.get('gidNumber', None)
  homeDirectory = request.META.get('homeDirectory', "/home/%s" % userid)
  loginShell = request.META.get('loginShell', "/bin/bash")

  uid = find_uid(uidNumber)
  gid = find_gid(gidNumber or uid)

  for user in users:
    if user.login_name == userid:
      return render(request, "errorName.html", {'user': user}) 
  
  newUser = LinuxUser.objects.create(
    login_name = userid,
    user_id = uid,
    group_id = gid,
    full_name = cn,
    home_directory = homeDirectory,
    login_shell = loginShell,
    user_state = 'I')

  newGroup = LinuxGroup.objects.create(
    group_name = userid or login_name,
    group_id = gid)

  return render(request, "register.html", { 'user': newUser })
