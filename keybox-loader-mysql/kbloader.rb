#!/usr/bin/env ruby

require 'yaml'

require 'sequel'
require 'jdbc/mysql'

Jdbc::MySQL.load_driver

user='keybox'
pass=''
dbhost='localhost'
dbport='3306'
dbname='keybox'

DB=Sequel.connect("jdbc:mysql://#{dbhost}:#{dbport}/#{dbname}?user=#{user}&password=#{pass}")

Dir.chdir(File.dirname(__FILE__))

HostsInfo = YAML.load(File.read('hosts.yml'))

class Host
  attr_reader :name, :info

  def initialize(name, info)
    @name = name
    @info = info
  end

  def to_s
    @name
  end
end

# remove hosts that are absent from the host.yml file but are still in the db
def remove_absent_hosts(db_conn)

  # grab all current hosts in the db
  current_hostlist=db_conn[:system].select(:id, :display_nm).map  { |h| h.values }

  # check for removed hosts
  hosts_to_remove = []

  current_hostlist.each do |name|
    unless HostsInfo[name[1]]
      hosts_to_remove << name[1]
    end
  end

  puts "Removing #{hosts_to_remove}"

  db_conn[:system].where(:display_nm => hosts_to_remove).delete
end

def each_host(&block)
  @hosts = HostsInfo.keys.sort

  @hosts.each do |name|
    yield Host.new(name, HostsInfo[name])
  end
end


def get_or_create_profile(db_conn, profile_name)
  profile_id = db_conn[:profiles].filter(:nm => profile_name).get(:id)
  if profile_id == nil
    profile_id = db_conn[:profiles].insert(:nm => profile_name, :descr => "Autocreated #{profile_name}")
    puts "Created new profile #{profile_name}"
  end

  profile_id
end

def populate_profile_with_all_hosts(db_conn, profile_id)
  # remove all hosts from profile
  db_conn[:system_map].where(:profile_id => "#{profile_id}").delete
  # retrieve all current hosts
  current_db_hosts = db_conn[:system].select(:id, :display_nm).map  { |h| h.values }
  # add each host to all hosts profile
  current_db_hosts.each do |host_id|
    db_conn[:system_map].insert(:profile_id => profile_id, :system_id => host_id[0])
  end
end

# START OF MAIN
####################################################################
authkey = DB[:application_key].get(:public_key)

DB.transaction do
  each_host do |host|
    ssh      = host.info["ssh"] or next
    hostname = ssh["HostName"] || ssh["Hostname"] || ssh["hostname"]
    port     = ssh["Port"] || ssh["port"] || 22
    user     = ssh["User"] || ssh["user"] || ("lookerops" if host.info["hosted"])

    #puts "Host #{host}\n"
    rec=DB[:system].where(:display_nm => "#{host}")
    if 1 != rec.update(:user=> "#{user}", :host => "#{hostname}", :port => port, :authorized_keys => "#{authkey}")
      #puts "Insert Host #{host}\n"
      DB[:system].insert(:display_nm => "#{host}", :user=> "#{user}", :host => "#{hostname}", :port => port, :authorized_keys => "#{authkey}")
    end
  end

  remove_absent_hosts(DB)

  all_hosts_profile_id = get_or_create_profile(DB, 'all_hosts')
  populate_profile_with_all_hosts(DB, all_hosts_profile_id)

  cs_hosts_profile_id = get_or_create_profile(DB, 'Customer Success')
  populate_profile_with_all_hosts(DB, cs_hosts_profile_id)

  dcl_hosts_profile_id = get_or_create_profile(DB, 'DCL')
  populate_profile_with_all_hosts(DB, dcl_hosts_profile_id)

  eng_hosts_profile_id = get_or_create_profile(DB, 'Engineering')
  populate_profile_with_all_hosts(DB, eng_hosts_profile_id)

  se_hosts_profile_id = get_or_create_profile(DB, 'Sales Engineering')
  populate_profile_with_all_hosts(DB, se_hosts_profile_id)

  ops_hosts_profile_id = get_or_create_profile(DB, 'Ops')
  populate_profile_with_all_hosts(DB, ops_hosts_profile_id)

  it_hosts_profile_id = get_or_create_profile(DB, 'IT')
  marketing_hosts_profile_id = get_or_create_profile(DB, 'Marketing')
end

# END OF MAIN
####################################################################