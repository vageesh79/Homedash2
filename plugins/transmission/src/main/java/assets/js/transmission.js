function transmission(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.torrents = [];

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

        var parent = this;

        rootElement(this.moduleId).on('click', '.btn.add', function () {
            parent.addTorrent();
        });
        rootElement(this.moduleId).on('click', '.btn.alt', function () {
            parent.setAltSpeed();
        });

        if (size == "full-screen") {
            rootElement(this.moduleId).on('click', '.pause-torrent', function () {
                parent.pauseTorrent(parent.selectedTorrent);
                $(".modal").modal('hide');
            });

            rootElement(this.moduleId).on('click', '.remove-torrent', function () {
                parent.removeTorrent(parent.selectedTorrent);
                $(".modal").modal('hide');
            });

            rootElement(this.moduleId).on('click', '.torrent', function (e) {
                $(".modal").modal('show');
                parent.selectedTorrent = $(this).attr('data');
            });
        }
    }


    this.onMessage_3x2 = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
        }
    };

    this.onMessage_2x2 = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
        }
    };

    this.onMessage_2x1 = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
        }
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
            this.displayTorrents(message);
        }
    }


    this.processData = function (json) {
        var root = rootElement(this.moduleId);

        if (!json) {
            root.find('.module-overlay').show();
        } else {
            root.find('.count').html(json.status.obj.map.torrentCount);
            root.find('.dl').html(this.humanFileSize(json.status.obj.map.downloadSpeed, true) + '/s');
            root.find('.ul').html(this.humanFileSize(json.status.obj.map.uploadSpeed, true) + '/s');

            var button = root.find('.alt')
            if (json.alternateSpeeds) {
                button.addClass("btn-primary");
                button.html('<span class="glyphicon glyphicon-sort-by-attributes-alt" aria-hidden="true"></span>');
            } else {
                button.removeClass("btn-primary");
                button.html('<span class="glyphicon glyphicon-sort-by-attributes" aria-hidden="true"></span>');
            }

            root.find('.module-overlay').hide();
        }
    }


    this.addTorrent = function (event) {
        var url = prompt("Margnet link", '');
        if (url != undefined) {
            if (url.length > 0) {
                sendMessage(this.moduleId, 'addTorrent', url);
            } else {
                showErrorMessage("Empty URL, can't add torrent");
            }
        }
    }

    this.setAltSpeed = function () {

        var button = rootElement(this.moduleId).find('.btn.alt');
        var setAltSpeed = true;
        if (button.hasClass("btn-primary")) {
            setAltSpeed = false;
            button.removeClass("btn-primary");
            button.html('<span class="glyphicon glyphicon-sort-by-attributes" aria-hidden="true"></span>');
        } else {
            setAltSpeed = true;
            button.addClass("btn-primary");
            button.html('<span class="glyphicon glyphicon-sort-by-attributes-alt" aria-hidden="true"></span>');
        }

        sendMessage(this.moduleId, 'altSpeed', setAltSpeed);
    }


    this.humanFileSize = function (bytes, si) {
        var thresh = si ? 1000 : 1024;
        if (bytes < thresh)
            return bytes + ' B';
        var units = si ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'] : [
            'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while (bytes >= thresh);
        return bytes.toFixed(1) + ' ' + units[u];
    }


//Full screen specific methods


    this.displayTorrents = function (json) {
        var parent = this;
        rootElement(parent.moduleId).find('.torrent-list').html('');
        $.each(json.torrents, function (index, value) {
            parent.torrents[value.id] = value;
            rootElement(parent.moduleId).find('.torrent-list').append(
                parent.torrentToHtml(value, json.rpcVersion));
        });

    }


    this.torrentToHtml = function (torrent, rpcVersion) {
        var html = [];

        var percent = Math.ceil(torrent.percentDone * 100);
        console.log(torrent.name + ":" + percent);

        html.push('<div  id="torrent-', torrent.id, '">');
        html.push('<p style="overflow: hidden; white-space: nowrap; text-overflow: ellipsis;">');
        html.push('<button  data="', torrent.id, '" class="torrent btn btn-primary btn-xs" style="float: right"><span class="glyphicon glyphicon-pencil"></span></button>');
        html.push(this.getStatusIcon(torrent.status, rpcVersion), ' ');
        html.push('<strong>', torrent.name, '</strong>');
        html.push('</p>');
        html.push('<div class="progress small-progress-bar">');
        html.push('<div class="progress-bar" role="progressbar" aria-valuenow="', percent, '" aria-valuemin="0" aria-valuemax="100" style="width: ', percent, '%;">');
        //html.push('<span class="sr-only">', percent, '% Complete</span>');
        html.push('</div>');
        html.push('</div>');
        html.push('<p>');
        html.push('DL: ', this.humanFileSize(torrent.downloadSpeed, true), '/s | Ul: ', this.humanFileSize(torrent.uploadSpeed, true), '/s');
        html.push('<span style="float:right">');
        html.push(this.humanFileSize(torrent.downloaded, true), '/', this.humanFileSize(torrent.totalSize, true));
        html.push('</span>');
        html.push('</p>');
        html.push('<hr />');

        return html.join("");
    }


    this.getStatusIcon = function (value, rpcVersion) {
        if (rpcVersion < 14) {
            switch (value) {
                case 16:
                    return '<i class="fa fa-check-square-o"></i>';
                case 8:
                    return '<i class="fa fa-cloud-upload"></i>';
                case 4:
                    return '<i class="fa fa-cloud-download"></i>';
                case 2:
                    return '<i class="fa fa-refresh"></i>';
                case 4:
                    return '<i class="fa fa-clock-o"></i>';
                default:
                    return '';
            }
        } else {
            switch (value) {
                case 6:
                    return '<i class="fa fa-cloud-upload"></i>';
                case 5:
                    return '<i class="fa fa-clock-o"></i>';
                case 4:
                    return '<i class="fa fa-cloud-download"></i>';
                case 3:
                    return '<i class="fa fa-clock-o"></i>';
                case 2:
                    return '<i class="fa fa-refresh"></i>';
                case 1:
                    return '<i class="fa fa-clock-o"></i>';
                case 0:
                    return '<i class="fa fa-pause"></i>';
                default:
                    return '';
            }
        }

    }

    this.removeTorrent = function (id) {
        sendMessage(this.moduleId, 'removeTorrent', id);
    }

    this.pauseTorrent = function (id) {
        sendMessage(this.moduleId, 'pauseTorrent', id);
    }

}