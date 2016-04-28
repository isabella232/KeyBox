#!/usr/bin/env ruby

require "yaml"

require 'sequel'
require 'jdbc/mysql'

Jdbc::MySQL.load_driver

user="keybox"
pass=""
dbhost="localhost"
dbport="3326"
dbname="keybox"

DB=Sequel.connect("jdbc:mysql://#{dbhost}:#{dbport}/#{dbname}?user=#{user}&password=#{pass}")

Dir.chdir(File.dirname(__FILE__))

HostsInfo = YAML.load(File.read("hosts.yml"))

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

def removed_hosts(hostlist)
  # check for removed hosts
  removed = []

  hostlist.each do |name|
    unless HostsInfo[name[1]]
      removed << name[1]
    end
  end
  return removed
end

def each_host(&block)
  @hosts = HostsInfo.keys.sort

  @hosts.each do |name|
    yield Host.new(name, HostsInfo[name])
  end
end

authkey=DB[:application_key].get(:public_key)

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

  dbhosts=DB[:system].select(:id, :display_nm).map  { |h| h.values }

  rmoved=removed_hosts(dbhosts)
  puts "Removed #{removed_hosts(dbhosts)}"

  DB[:system].where(:display_nm => rmoved).delete

  all_profile=DB[:profiles].filter(:nm => "all_hosts").get(:id)
  if all_profile == nil
    all_profile=DB[:profiles].insert(:nm => "all_hosts", :descr => "Autocreated All Hosts")
  end

  dbhosts=DB[:system].select(:id, :display_nm).map  { |h| h.values }
  DB[:system_map].where(:profile_id => "#{all_profile}").delete
  dbhosts.each do |host_id|
    DB[:system_map].insert(:profile_id => all_profile, :system_id => host_id[0])
  end
end
